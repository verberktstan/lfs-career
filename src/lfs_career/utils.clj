(ns lfs-career.utils
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]))

(defn validate [spec x]
  (if (s/valid? spec x)
    x
    (throw (ex-info (s/explain-str spec x) (s/explain-data spec x)))))

(defn check [pred x s]
  (if (pred x) x (throw (ex-info s x))))

(defn spec-keys [spec]
  (let [description (s/describe spec)]
    (when (#{'keys} (first description))
      (reduce
       (fn [result [k v]]
         (cond-> result
           (#{:req :opt :req-un :opt-un} k) (set/union (set v))))
       #{}
       (partition 2 (rest description))))))

(defn key-by [f coll]
  (into {} (map #(vector (f %) %) coll)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; LFS domain stuff

(defn race-in-progress? [sta]
  (-> sta :race-in-progress #{:race}))

(defn no-race-in-progress? [sta]
  (-> sta :race-in-progress #{:no-race}))

(defn qualifying-in-progress? [sta]
  (-> sta :race-in-progress #{:qualifying}))
