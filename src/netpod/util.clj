(ns netpod.util)

(defn ret-ex-as-value
  "function will return exception as a value"
  [f & args]
  (try
    (apply f args)
    (catch java.lang.Exception e e)))
