#!/usr/bin/env bb

(require '[netpod.net :refer [send-msg]]
         '[netpod.pods :as pods]
         '[clojure.core.async :refer [<!]])

(comment (prn (String. (send-msg "./server/server.sock" {"op" "describe"}))))
(pods/load-pod "./server/server.sock")
;;check pod namespace exists
(println "sample.service ns exists =>" (some? (find-ns 'sample.service)))
(require '[sample.service :as srv])

(let [ch (srv/greet "john")
      result (<! ch)]
  (prn "got from server" result))

;;(srv/greet)

