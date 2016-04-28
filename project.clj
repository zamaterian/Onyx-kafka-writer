(defproject batch-vur "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [mount "0.1.10"]

                [org.clojure/tools.cli "0.3.3"]
                 [org.onyxplatform/onyx-seq "0.9.4.0"]
                 [org.onyxplatform/onyx-kafka "0.9.4.0"]
                 [org.onyxplatform/onyx "0.9.4"]
                 ]

  :jvm-opts ["-DappName=batch_vur"]

  :profiles {:dev {:source-paths ["dev"]}
             :uberjar {:aot  :all
                       :main batch-vur.system}})
