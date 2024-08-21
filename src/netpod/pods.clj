(ns netpod.pods
  (:require [cheshire.core :as json]
            [netpod.exec :as exec]
            [netpod.net :as net])
  (:import [java.util UUID]))

(defn generate-uuid []
  (str (UUID/randomUUID)))

(defn make-fn
  "creates a given function in the given namespace"
  [path ns-symbol var-desc]
  (let [var-name (get var-desc "name")
        var-sym (symbol var-name)
        ns-name (str ns-symbol)]
    (intern ns-symbol var-sym (fn [& args]
                                ;;encode args to json
                                ;;craft invoke request
                                ;;send it via executor
                                (let [enc-args (json/generate-string args)
                                      req {:op "invoke"
                                           :id (generate-uuid)
                                           :var (format "%s/%s" ns-name var-name)
                                           :args enc-args}]
                                  (exec/async-send #(net/send-msg path req)))))))

(defn make-ns
  "make namespace as provided via the describe msg received"
  [path ns-desc]
  (prn "making" ns-desc)
  (let [ns-symbol (symbol (get ns-desc "name"))
        vars (get ns-desc "vars")]
    (create-ns ns-symbol)
    (doseq [v vars]
      (make-fn path ns-symbol v))))

(defn load-pod
  "loads netpod via the provided unix socket"
  [path]
  (let [data (net/send-msg path {"op" "describe"})]
    (prn data)
    (doall (map #(make-ns path %) (get data "namespaces")))))
