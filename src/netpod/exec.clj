(ns netpod.exec
  (:require [clojure.core.async :as a :refer [>!!]])
  (:import [java.util.concurrent Executors]))

(defonce executor (Executors/newVirtualThreadPerTaskExecutor))

(defn send-cb
  "sends the function f to executor cb will be called with the result produced by f when it becomes available"
  [f cb & args]
  (.submit executor #(cb (apply f args))))

(defn async-send
  "Executes f with args in another thread, returning immediately to the
calling thread. Returns a channel which will receive the result of
  the f when completed, then close."
  [f & args]
  (let
   [ch (a/chan)]
    (apply send-cb f (fn [res]
                       (>!! ch res)
                       (a/close! ch)) args)
    ch))
