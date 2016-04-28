(ns batch-vur.system
  (:require [batch-vur.core :as core]
            [aero.core :refer (read-config)]
            [clojure.java.io :as io]
            [mount.core :refer [defstate] :as mount])
  (:gen-class))

(defstate config
          :start (read-config (io/resource "config.edn") {:profile :docker}))


(defstate onyx-system
          :start (core/start-onyx config)
          :stop (core/shutdown-onyx onyx-system))

#_(defstate jobs
          :start (core/submit-jobs! onyx-system config))

(defn -main[]
   (mount/start))
