(ns lfs-career.grid
  (:require [lfs-career.utils :as u]
            [clojure.spec.alpha :as s]))

(s/def ::model (s/coll-of string? :max-count 32))

(defn generate [n]
  {:post [(u/validate ::model %)]}
  (let [n (min n 32)]
    (->> (range 1 33) (shuffle) (take n) (map (partial str "AI ")))))
