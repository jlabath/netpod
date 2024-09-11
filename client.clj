#!/usr/bin/env bb

(require '[netpod.pods :as pods])

(pods/with-pod "./server/server"
  ;;if require is not suitable can also resolve things dynamically e.g. sample.service/greet
  (prn "basic sync response from server" ((ns-resolve 'sample.service 'greet) "abe"))
  ;;use require
  (require
   '[clojure.core.async :as a :refer [<!]]
   '[sample.service :as srv])

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
      (println "Received:" @r))))

