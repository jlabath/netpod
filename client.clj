#!/usr/bin/env bb

(require '[netpod.net :refer [send-msg]]
         '[netpod.pods :as pods]
         '[clojure.core.async :as a :refer [<!]]
         '[babashka.process :refer [process]]
         '[clojure.java.io :as io])

(def socket-path "/tmp/sample-service.sock")

;; Start a process to run in the background
(def background-process (process ["./server/server" socket-path] {:wait false}))

(loop  []
  (when (not (.exists (io/file socket-path)))
    (Thread/sleep 100)
    (recur)))


;;load pod from netpod
(pods/load-pod socket-path)

(def lazy-code
  '(do
     (require '[sample.service :as srv])
     (let [ch (srv/greet "john")
           result (<! ch)]
       (prn "got from server" result))
     (let [ch (srv/broken-func "fred")
           result (<! ch)]
       (prn "got from server" result))
     ;;now run many requests
     (let [size 5000
      numbers (range size)
      chs (into [] (for [num numbers] (srv/greet (str "channel task" num))))
      ch (a/merge chs)]
       (loop []
         (if-let [value (<! ch)]
           (do
             (println "Received:" value)
             (recur))
           (println "Channel closed"))))))

;;call it if we have the namespace
(when (some? (find-ns 'sample.service))
  (eval lazy-code))

