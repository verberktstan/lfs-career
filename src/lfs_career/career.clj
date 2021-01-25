(ns lfs-career.career
  (:require [lfs-career.season :as season]
            [lfs-career.utils :as u]
            [clojure.spec.alpha :as s]))

(s/def ::seasons (s/map-of keyword? ::season/model))
(s/def ::unlocked-seasons (s/coll-of keyword? :kind set?))
(s/def ::unlocked-cars (s/coll-of string? :kind set?))

(s/def ::model
  (s/keys :req [::unlocked-cars ::unlocked-seasons ::seasons]))

(defn key-by [f coll]
  (into {} (map #(vector (f %) %) coll)))

(defn make [{:keys [seasons unlocked-seasons unlocked-cars]}]
  {:post [(u/validate ::model %)]}
  {::seasons (key-by ::season/key seasons)
   ::unlocked-cars unlocked-cars
   ::unlocked-seasons unlocked-seasons})

(defn- season-active? [career]
  (s/valid? ::season/model career))

(defn start-season [{::keys [seasons] :as career} season-key]
  (when (season-active? career)
    (throw (ex-info "A season is active" career)))
  (let [season (u/validate ::season/model (get seasons season-key))]
    (merge career season)))
