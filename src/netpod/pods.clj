(ns netpod.pods
  (:require [cheshire.core :as json]
            [netpod.exec :as exec]
            [netpod.net :as net]
            [netpod.util :refer [ret-ex-as-value]])
  (:import [java.util UUID]))

(defn generate-uuid []
  (str (UUID/randomUUID)))

(defn send-req
  "sends a request via send-msg and the parses the response"
  [path req]
  (let [response (net/send-msg path req)]
    (case (get response "status")
      "done" (-> (get response "value") json/parse-string)
      "error" (ex-info (get response "ex-message") (get response "ex-data" {}))
      (ex-info "invalid response" response))))

(defn make-fn
  "creates a given function in the given namespace"
  [path ns-symbol var-desc]
  (let [var-name (get var-desc "name")
        var-sym (symbol var-name)
        chan-var-sym (symbol (format "%s-chan" var-name))
        ns-name (str ns-symbol)
        fn-body-chan (partial ret-ex-as-value send-req path)
        base-req {:op "invoke"
                  :var (format "%s/%s" ns-name var-name)}]
    (intern ns-symbol chan-var-sym (fn [& args]
                                ;;encode args to json
                                ;;craft invoke request
                                ;;send it via executor
                                     (let [enc-args (json/generate-string args)
                                           req (assoc base-req :id (generate-uuid) :args enc-args)]
                                       (exec/async-send #(fn-body-chan req)))))
    (intern ns-symbol var-sym (fn [& args]
                                ;;encode args to json
                                ;;craft invoke request
                                ;;send it via sync-send
                                (let [enc-args (json/generate-string args)
                                      req (assoc base-req :id (generate-uuid) :args enc-args)]
                                  (exec/sync-send #(fn-body-chan req)))))))

(defn make-ns
  "make namespace as provided via the describe msg received"
  [path ns-desc]
  ;;(prn "making" ns-desc)
  (let [ns-symbol (symbol (get ns-desc "name"))
        vars (get ns-desc "vars")]
    (create-ns ns-symbol)
    (doseq [v vars]
      (make-fn path ns-symbol v))))

(defn load-pod
  "loads netpod via the provided unix socket"
  [path]
  (try
    (let [data (net/send-msg path {"op" "describe"})]
      ;;(prn data)
      (doall (map #(make-ns path %) (get data "namespaces"))))
    (catch java.lang.Exception e e)))
