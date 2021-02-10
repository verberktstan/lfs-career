(ns lfs-career.test-helpers
  (:require [clojure.test :refer [is]]))

(def is-thrown-ex? #(is (thrown? Exception (%))))
