(ns batch-vur.core
  (:gen-class)
  (:require [clojure.core.async :refer [chan >!! <!! close!]]
            [onyx.plugin.core-async :refer [take-segments!]]
            [cheshire.core :as json]
            [aero.core :refer (read-config)]
            [onyx.plugin.kafka]
            [onyx.plugin.seq]
            [onyx.api]))

(defn serialize-message-json [segment]
  (.getBytes (json/generate-string segment)))

(def lock (Object.))

(defn sync-logger  [str]
  (locking  lock
     (println str)))

(defn transform-into-beregn-msg [ejendom-segment]
  (let [msg  {:message (merge ejendom-segment  {:id (java.util.UUID/randomUUID)})}]
    (sync-logger (str "vur-ejd-id=" (:vur-ejd-id ejendom-segment) ", id=" (get-in msg [:message :id])))
    msg))

(def workflow
  [[:in :beregn-msg]
  #_[:beregn-msg :out]
   [:beregn-msg :write-messages]])


(def capacity 1000)

(def output-chan (chan capacity))

(def batch-size 10)

(defn catalog [{:keys [topic kafka-zookeeper]} ]
  [{:onyx/name :in
	:onyx/plugin :onyx.plugin.seq/input
	:onyx/type :input
	:onyx/medium :seq
	:seq/checkpoint? true
	:onyx/batch-size batch-size
	:onyx/max-peers 1
	:onyx/doc "Reads segments from seq"}

   {:onyx/name :beregn-msg
	:onyx/fn ::transform-into-beregn-msg
	:onyx/type :function
	:onyx/batch-size batch-size}

   {:onyx/name :write-messages
	:onyx/plugin :onyx.plugin.kafka/write-messages
	:onyx/type :output
	:onyx/medium :kafka
	:kafka/topic   topic
    :kafka/zookeeper kafka-zookeeper
    :kafka/serializer-fn ::serialize-message-json
	:kafka/request-size 307200
	:onyx/batch-size batch-size
	:onyx/doc "Writes messages to a Kafka topic"}
   ])



(defn inject-out-ch [event lifecycle]
  {:core.async/chan output-chan})

(def out-calls
  {:lifecycle/before-task-start inject-out-ch})

(defn inject-in-reader [event lifecycle]
  (prn "filename "  (:buffered-reader/filename lifecycle) (type (clojure.java.io/resource (:buffered-reader/filename lifecycle))) )
  (let [rdr (clojure.java.io/reader (clojure.java.io/resource (:buffered-reader/filename lifecycle)))]
    {:seq/rdr rdr
     :seq/seq (take 10000 (map #(assoc {} :vur-ejd-id (Integer. %)) (line-seq (java.io.BufferedReader. rdr))))}))

(defn close-reader [event lifecycle]
  (println "Closing reader !!!")
  (.close (:seq/rdr event)))

(def in-calls-seq
  {:lifecycle/before-task-start inject-in-reader
   :lifecycle/after-task-stop close-reader})

(def lifecycles
  [
   {:lifecycle/task :in
	:buffered-reader/filename "vur-ejd-ids.edn"
	:lifecycle/calls ::in-calls-seq}

   {:lifecycle/task :in
	:lifecycle/calls :onyx.plugin.seq/reader-calls}

   {:lifecycle/task :write-messages
	:lifecycle/calls :onyx.plugin.kafka/write-messages-calls}
])

(defn shutdown-onyx [onyx-system]
  (doseq [p (:virtual-peers onyx-system)]
    (onyx.api/shutdown-peer p))
  (onyx.api/shutdown-peer-group (:peer-group onyx-system)))

(defn close-peer-on-job-completion [system-config job-id]
  (future (onyx.api/await-job-completion (:config (:peer-group system-config)) job-id)
          (prn "job-id: " job-id", is now finished! Exiting.")
          (shutdown-onyx system-config)
          ; waiting for aeron to cleanup
          (Thread/sleep 500)
          (System/exit 0)))


(defn submit-jobs! [peer-config config]
  (let [jobs (onyx.api/submit-job
               (:config (:peer-group peer-config))
               {:catalog (catalog config) :workflow workflow :lifecycles lifecycles
                :task-scheduler :onyx.task-scheduler/balanced})]
    #_ (close-peer-on-job-completion peer-config (:job-id jobs))

    jobs))


(defn start-onyx [config]
  (prn "tenancy-id:" (:onyx/tenancy-id (:peer-config config)) "host ip:" (:onyx.messaging/bind-addr (:peer-config config)))

  (let [peer-group (onyx.api/start-peer-group (:peer-config config))
        virtual-peers (onyx.api/start-peers (:number-of-peers config) peer-group #_monitoring-config)]
    (println "Onyx peer is up...")
    {:peer-group peer-group :virtual-peers virtual-peers}))


(defn -main []
  (let [config (read-config (clojure.java.io/resource "config.edn") {:profile :docker})
        peer-config (get config :peer-config)
        job-config  {:catalog (catalog config) :workflow workflow :lifecycles lifecycles
                     :task-scheduler :onyx.task-scheduler/balanced}
        _ (prn peer-config job-config)
        jobs (onyx.api/submit-job
               peer-config
               {:catalog (catalog config) :workflow workflow :lifecycles lifecycles
                :task-scheduler :onyx.task-scheduler/balanced}) ]

   (prn jobs)
   (prn "submittet")
   ))
