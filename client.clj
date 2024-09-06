#!/usr/bin/env bb

(require '[netpod.net :refer [send-msg]]
         '[netpod.pods :as pods]
         '[clojure.core.async :as a :refer [<!]]
         '[babashka.process :refer [process]]
         '[clojure.java.io :as io])

(def socket-path "/tmp/sample-service.sock")

;; Start a process to run in the background
(def background-process (process ["./server/server" socket-path] {:wait false}))

(loop []
  (when (not (.exists (io/file socket-path)))
    (Thread/sleep 100)
    (recur)))

;;load pod from netpod
(pods/load-pod socket-path)

(def lazy-code
  '(do
     (require '[sample.service :as srv])
     (prn "basic sync response from server" (srv/greet "abe"))
     (prn "basic sync response from server" @(srv/greet-delay "slow pete"))
     (let [ch (srv/greet-chan "john")
           result (<! ch)]
       (prn "got from server" result))
     (let [ch (srv/broken-func-chan "fred")
           result (<! ch)]
       (prn "got from server" result))
     ;;now run many requests
     (let [size 1000
           numbers (range size)
           chs (into [] (for [num numbers] (srv/greet-chan (str "channel task" (inc num)))))
           ch (a/merge chs)]
       (loop []
         (if-let [value (<! ch)]
           (do
             (println "Received:" value)
             (recur))
           (println "Channel closed"))))
     ;;now run many via requests
     (println "doing the delay")
     (let [size 1000
           numbers (range size)
           ps (into [] (for [num numbers] (srv/greet-delay (str "delay task" (inc num)))))]
       (doseq [r ps]
         (println "Received:" @r)))))

;;call it if we have the namespace
(when (some? (find-ns 'sample.service))
  (eval lazy-code))

