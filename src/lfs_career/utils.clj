(ns lfs-career.utils
  (:require [clojure.spec.alpha :as s]))

(defn validate [spec x]
  (if (s/valid? spec x)
    x
    (throw (ex-info (s/explain-str spec x) (s/explain-data spec x)))))

(defn check [pred x s]
  (if (pred x) x (throw (ex-info s x))))
