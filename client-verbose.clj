#!/usr/bin/env bb

(require '[netpod.pods :as pods]
         '[netpod.exec :refer [promise-task]])

;;demonstrates the inner workings of netpod use in greater details
;;in case the with-netpod macro is not suitable

(def socket-path "/tmp/sample-service.sock")

;; start a process to run in the background
(def netpod-process (pods/start-pod "./server/server" socket-path 2000))

;;load pod from netpod
(pods/load-pod socket-path)

(def lazy-code
  '(do
     (require
      '[clojure.core.async :as a :refer [<!]]
      '[sample.service :as srv])
     (prn "basic sync response from server" @(srv/greet "abe"))

     (let [result (srv/broken-func "fred")]
       (prn "got from server" @result))
     ;;now run many via requests
     (println "doing the delay")
     (time (let [size 2000
                 numbers (range size)
                 ps (into [] (for [num numbers] (srv/greet (str "delay task" (inc num)))))]
             (doseq [r ps]
               (println "Received:" @r))))))

;;call it if we have the namespace
(when (some? (find-ns 'sample.service))
  (eval lazy-code))

(let
 [p (promise-task
     (println "I AM IN ANOTHER THREAD")
     (flush)
     (Thread/sleep 500)
     (inc 41))]
  (println (format "result is %d" @p)))
;;turn off the child process at the end
(pods/stop-pod netpod-process)
