(ns netpod.exec
  (:import [java.util.concurrent Executors]))

(defonce executor (Executors/newVirtualThreadPerTaskExecutor))

(defn send-cb
  "sends the function f to executor cb will be called with the result produced by f when it becomes available"
  [f cb & args]
  (.submit executor #(cb (apply f args))))

(defn promise-send
  "Executes f with args in another thread delivering result to a promise once it is available, delay-send returns that promise to caller."
  [f & args]
  (let
   [p (promise)]
    (apply send-cb f #(deliver p %) args)
    p))

(defmacro promise-task
  "similar to future from standard library except it returns a promise"
  [& body]
  `(let [f# (fn [] (try
                     (do ~@body)
                     (catch java.lang.Exception ex ex)))]

     (promise-send f#)))
