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

(defn make [{:keys [seasons unlocked-seasons unlocked-cars]}]
  {:post [(u/validate ::model %)]}
  {::seasons (u/key-by ::season/key seasons)
   ::unlocked-cars unlocked-cars
   ::unlocked-seasons unlocked-seasons})

(defn- active-season [career]
  (and (s/valid? ::season/model career) career))

(defn list-unlocked [{::keys [unlocked-cars unlocked-seasons]}]
  [(str "Unlocked cars: " (str/join ", " unlocked-cars))
   (str "Unlocked seasons: " (str/join ", " (map name unlocked-seasons)))])

(defn start-season [{::keys [seasons unlocked-seasons] :as career} season-key]
  (when-not (contains? unlocked-seasons season-key)
    (throw (ex-info (str "Season " (name season-key) " is not available!") career)))
  (when-let [active-season (active-season career)]
    (when-not (= season-key (::season/key active-season))
      (throw (ex-info "A nother season is active!" career))))
  (let [season (u/validate ::season/model (get seasons season-key))]
    (merge season career)))

(defn end-season [career]
  (when-not (active-season career)
    (throw (ex-info "No season is active!" career)))
  (let [{::season/keys [unlocks] :as ended-career} (season/end career)]
    (when-not (season/finished? ended-career)
      (throw (ex-info "Season is not finished yet!" career)))
    (merge-with set/union
     (apply dissoc ended-career (u/spec-keys ::season/model))
     unlocks)))

(defn available-cars [{::keys [unlocked-cars] :as career
                       ::season/keys [cars]}]
  (cond-> unlocked-cars
    (seq cars) (set/intersection cars)))
