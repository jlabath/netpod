(ns netpod.pods (:require [netpod.net :as net]))

(defn make-fn
  "creates a given function in the given namespace"
  [ns-symbol var-desc]
  (let [var-sym (symbol (get var-desc "name"))]
    (intern ns-symbol var-sym (fn [& args] (prn args)))))

(defn make-ns
  "make namespace as provided via the describe msg received"
  [ns-desc]
  (prn "making" ns-desc)
  (let [ns-symbol (symbol (get ns-desc "name"))
        vars (get ns-desc "vars")]
    (create-ns ns-symbol)
    (doseq [v vars]
      (make-fn ns-symbol v))))

(defn load-pod
  "loads netpod via the provided unix socket"
  [path]
  (let [data (net/send-msg path {"op" "describe"})]
    (prn data)
    (doall (map make-ns (get data "namespaces")))))
