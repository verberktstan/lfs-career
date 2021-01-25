(ns lfs-career.utils
  (:require [clojure.spec.alpha :as s]))

(defn validate [spec x]
  (if-not (s/valid? spec x)
    (throw (ex-info (s/explain-str spec x) (s/explain-data spec x)))
    :valid))
