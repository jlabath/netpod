#!/usr/bin/env bb

(require '[netpod.pods :as pods])

(pods/with-pod "./server/server"
  ;; require is not suitable in macros
  ;; but one can also resolve things dynamically using resolve such as below
  (prn "basic sync response from server" @((resolve 'sample.service/greet) "slow pete"))
  (let [result ((resolve 'sample.service/greet) "john")]
    (prn "got from server" @result))
  (let [result ((resolve 'sample.service/broken-func) "fred")]
    (prn "got from server" @result))
  ;;now run many via requests
  (time (let [size 1000
              numbers (range size)
              ps (into [] (for [num numbers] ((resolve 'sample.service/greet) (str "task" (inc num)))))]
          (doseq [r ps]
            (println "Received:" @r)))))

