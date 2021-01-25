(ns lfs-career.season
  (:require [lfs-career.cars :as cars]
            [lfs-career.grid :as grid]
            [lfs-career.race :as race]
            [lfs-career.tracks :as tracks]
            [lfs-career.utils :as u]
            [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(s/def ::cars (s/coll-of cars/ALL :kind set?))
(s/def ::grid ::grid/model)
(s/def ::grid-size pos-int?)
(s/def ::key keyword?)
(s/def ::race ::race/model)
(s/def ::races (s/coll-of ::race/model))
(s/def ::n-races pos-int?)
(s/def ::results coll?)
(s/def ::result map?)
(s/def ::unlocks (s/nilable map?))

(s/def ::model
  (s/keys :req [::cars ::grid-size ::n-races ::key ::races ::results ::unlocks]
          :opt [::grid ::race ::result]))

(defn make [{:keys [cars grid-size key races unlocks]}]
  {:post [(u/validate ::model %)]}
  {::key key
   ::cars (or cars #{"UF1"})
   ::grid-size (or grid-size 20)
   ::races (or races [(race/make {:track "BL1"})])
   ::n-races (if races (count races) 1)
   ::unlocks unlocks
   ::results []})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Private functions

(defn- initialized? [{::keys [grid]}]
  grid)

(defn- generate-grid [{::keys [grid-size] :as season}]
  (assoc season ::grid (grid/generate (dec grid-size))))

(defn- initialize [season]
  (cond-> season
    (not (initialized? season))
    (generate-grid)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public functions

(defn register-result [season result]
  (update season ::result conj result))

(defn next-race [{::keys [races result] :as season}]
  (when-not (seq races)
    (throw (ex-info "No more races left in season!" season)))
  (when-not result
    (throw (ex-info "No race result yet for current race" season)))
  (-> (initialize season)
      (dissoc ::result)
      (update ::results conj result)
      (update ::races rest)
      (merge (first races))))
