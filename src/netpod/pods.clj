(ns netpod.pods
  (:require [babashka.fs :as fs]
            [babashka.process :as process :refer [process]]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [netpod.exec :as exec]
            [netpod.net :as net]
            [netpod.util :refer [ret-ex-as-value]])
  (:import [java.util UUID]))

(defn- generate-uuid []
  (str (UUID/randomUUID)))

(defn- send-req
  "sends a request via send-msg and the parses the response"
  [path req]
  (let [response (net/send-msg path req)]
    (case (get response "status")
      "done" (-> (get response "value") json/parse-string)
      "error" (ex-info (get response "ex-message") (get response "ex-data" {}))
      (ex-info "invalid response" response))))

(defn- make-fn
  "creates a given function in the given namespace"
  [path ns-symbol var-desc]
  (let [var-name (get var-desc "name")
        var-sym (symbol var-name)
        ns-name (str ns-symbol)
        fn-body-chan (partial ret-ex-as-value send-req path)
        base-req {:op "invoke"
                  :var (format "%s/%s" ns-name var-name)}]
    (intern ns-symbol var-sym (fn [& args]
                                ;;encode args to json
                                ;;craft invoke request
                                ;;send it via promise-send
                                (let [enc-args (json/generate-string args)
                                      req (assoc base-req :id (generate-uuid) :args enc-args)]
                                  (exec/promise-send #(fn-body-chan req)))))))

(defn- make-ns
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

(defn start-pod
  "starts the netpod via shell as a subprocess 
   the pod process is expected to accept the socket path as its first argument
   this function returns the object representing the running pod
   or throws an exception if there is trouble
  "
  [pod-path socket-path timeout-ms]
  (.delete (io/file socket-path))
  (let [prom (promise)
        background-proc (process [pod-path socket-path]
                                 {:wait false
                                  :out :inherit
                                  :err :inherit})]
    (exec/promise-send
     (fn []

       (loop []
         (when (not (.exists (io/file socket-path)))
           (Thread/sleep 50)
           (recur)))
       (deliver prom background-proc)))
    (if (= :timeout (deref
                     prom
                     timeout-ms
                     :timeout))
      (throw (ex-info "timed out while waiting for pod to start" {:pod-executable pod-path
                                                                  :socket socket-path}))
      {:process @prom
       :socket socket-path
       :pod-executable pod-path})))

(defn stop-pod
  "stops the netpod specified and kills the running netpod subprocess"
  [netpod]
  (process/destroy (:process netpod)))

(defmacro with-pod
  "Convenience macro that will combine `start-pod`, `load-pod` to ensure pod namespaces are loaded,
  before evaluating the body.
  It will also stop the process returned by `start-pod` upon exit.
  Since the namespaces are created dynamically at runtime please use (resolve 'my.netpod/some-func),
  instead of directly refering to symbols that may not yet exist."
  [pod-executable & body]
  `(let [temp-file# ~(fs/create-temp-file
                      {:dir "/tmp"
                       :prefix "netpod"
                       :suffix ".sock"})
         temp-file-path# (.toString temp-file#)
         pod# (start-pod ~pod-executable temp-file-path# 5000)]
     (try
       (load-pod temp-file-path#)
       (do ~@body)
       (finally
         (stop-pod pod#)
         (io/delete-file temp-file-path# :silently)))))
