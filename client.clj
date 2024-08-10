#!/usr/bin/env bb

(require '[netpod.net :refer [send-msg]])

(prn (String. (send-msg "./server/server.sock" "little john")))
