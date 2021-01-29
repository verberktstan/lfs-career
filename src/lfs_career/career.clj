(ns lfs-career.career
  (:require [lfs-career.season :as season]
            [lfs-career.utils :as u]
            [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]))

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

(defn list-unlocked [{::keys [unlocked-cars unlocked-seasons]}]
  [(str "Unlocked cars: " (str/join ", " unlocked-cars))
   (str "Unlocked seasons: " (str/join ", " (map name unlocked-seasons)))])

(defn start-season [{::keys [seasons unlocked-seasons] :as career} season-key]
  (when-not (contains? unlocked-seasons season-key)
    (throw (ex-info (str "Season " (name season-key) " is not available!") career)))
  (when (season-active? career)
    (throw (ex-info "A season is active!" career)))
  (let [season (u/validate ::season/model (get seasons season-key))]
    (merge career season)))

(defn end-season [career]
  (when-not (season-active? career)
    (throw (ex-info "No season is active!" career)))
  (when-not (season/finished? career)
    (throw (ex-info "Season is not finished yet!" career)))
  (let [{::season/keys [unlocks]} (season/end career)]
    (merge-with set/union
     (dissoc career ::season/key)
     unlocks)))