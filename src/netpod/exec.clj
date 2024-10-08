(ns netpod.exec
  (:require [clojure.core.async :as a :refer [>!! <!!]])
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

(defn sync-send
  "Executes f with args in another thread, blocking until the thread returns the result of (apply f args)."
  [f & args]
  (let
   [ch (apply async-send f args)]
    (<!! ch)))

(defn delay-send
  "Executes f with args in another thread delivering result to a promise once it is available, delay-send returns that promise to caller."
  [f & args]
  (let
   [p (promise)]
    (apply send-cb f #(deliver p %) args)
    p))

(defmacro promise-task
  "similar to future from standard library except it returns a promise"
  [& body]
  `(let [f# (fn [] (do ~@body))]
     (delay-send f#)))
