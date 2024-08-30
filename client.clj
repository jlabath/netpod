#!/usr/bin/env bb

(require '[netpod.net :refer [send-msg]]
         '[netpod.pods :as pods]
         '[clojure.core.async :refer [<!]])

(pods/load-pod "./server/server.sock")

(def lazy-code
  '(do
     (require '[sample.service :as srv])
     (let [ch (srv/greet "john")
           result (<! ch)]
       (prn "got from server" result))
     (let [ch (srv/broken-func "fred")
           result (<! ch)]
       (prn "got from server" result))))

;;call it if we have the namespace
(when (some? (find-ns 'sample.service))
  (eval lazy-code))


