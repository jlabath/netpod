(ns netpod.pods (:require [netpod.net :as net]))

(defn load-pod
  "loads netpod via the provided unix socket"
  [path]
  (let [data (net/send-msg path {"op" "describe"})]
    (prn data)))
