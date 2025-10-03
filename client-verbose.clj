#!/usr/bin/env bb

(require '[netpod.pods :as pods]
         '[netpod.exec :refer [promise-task]])

;;demonstrates the inner workings of netpod use in greater details
;;in case the with-netpod macro is not suitable

(def socket-path "/tmp/sample-service.sock")

;; start a process to run in the background
(def netpod-process (pods/start-pod "./server/server" socket-path 2000 {"SOME_VAR" "hello there i got this extra env"}))

;;load pod from netpod
(pods/load-pod socket-path)
(require '[sample.service :as srv])

(prn "basic sync response from server" @(srv/greet "abe"))
(let [result (srv/broken-func "fred")]
  (prn "got from server" @result))

;;many requests
(time (let [size 1000
            numbers (range size)
            ps (into [] (for [num numbers] (srv/greet (str "task" (inc num)))))]
        (doseq [r ps]
          (println "Received:" @r))))

(let
 [num 41
  p (promise-task
     (println "promise task sleeping")
     (flush)
     (Thread/sleep 500)
     (inc num))]
  (println (format "result is %d" @p)))

;;turn off the child process at the end
(pods/stop-pod netpod-process)
