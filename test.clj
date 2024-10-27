#!/usr/bin/env bb

(require

 '[clojure.test :refer [deftest is run-tests]]
 '[netpod.exec :as ne])

(deftest test-promise-task
  (is (= 5 (deref (ne/promise-task (+ 2 3)) 1000 :timeout))))

(deftest test-promise-task-exception
  (is (instance? Throwable (deref (ne/promise-task (/ 4 0)) 1000 :timeout))))

(println (run-tests))
