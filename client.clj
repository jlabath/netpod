#!/usr/bin/env bb

(require '[netpod.net :refer [send-msg]]
         '[netpod.pods :as pods])

(comment (prn (String. (send-msg "./server/server.sock" {"op" "describe"}))))
(pods/load-pod "./server/server.sock")
