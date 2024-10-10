#!/usr/bin/env bb

(require '[netpod.pods :as pods])

(pods/with-pod "./server/server"
  ;;if require is not suitable can also resolve things dynamically e.g. sample.service/greet
  (prn "basic sync response from server" ((ns-resolve 'sample.service 'greet) "abe"))
  ;;use require
  (require
   '[sample.service :as srv])

  (prn "basic sync response from server" @(srv/greet "slow pete"))
  (let [result (srv/greet "john")]
    (prn "got from server" @result))
  (let [result (srv/broken-func "fred")]
    (prn "got from server" @result))
  ;;now run many via requests
  (time (let [size 2000
              numbers (range size)
              ps (into [] (for [num numbers] (srv/greet (str "task" (inc num)))))]
          (doseq [r ps]
            (println "Received:" @r)))))

