(ns lfs-career.season
  (:require [lfs-career.cars :as cars]
            [lfs-career.grid :as grid]
            [lfs-career.race :as race]
            [lfs-career.result :as result]
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
(s/def ::results (s/nilable (s/coll-of (s/coll-of ::result/model))))
(s/def ::unlocks (s/nilable (s/map-of #{:lfs-career.career/unlocked-cars
                                        :lfs-career.career/unlocked-seasons}
                                      set?)))

(s/def ::model
  (s/keys :req [::cars ::grid-size ::n-races ::key ::races ::results ::unlocks]
          :opt [::grid ::race]))

(defn make [{:keys [cars grid-size key races unlocks]}]
  {:post [(u/validate ::model %)]}
  {::key key
   ::cars (or cars #{"UF1"})
   ::grid-size (or grid-size 20)
   ::races (or races [(race/make {:track "BL1"})])
   ::n-races (if races (count races) 1)
   ::unlocks unlocks
   ::results nil})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Private functions

(defn- generate-grid [{::keys [grid-size] :as season}]
  (assoc season ::grid (grid/generate (dec grid-size))))

(defn- initialized?
  "A season that has an actual grid is considered to be initialized."
  [{::keys [grid]}]
  grid)

(defn- register-race-results
  "If race results are present, move the data to the season/results collection."
  [{::race/keys [results] :as season}]
  (cond-> season
    (seq results) (assoc ::race/results nil)
    (seq results) (update ::results #(conj % results))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public functions

(defn end [{::race/keys [results] :as season}]
  (register-race-results season))

(defn next-race [{::keys [grid-size races] :as season}]
  (when-not (seq races)
    (throw (ex-info "No more races left in season!" season)))
  (let [new-season (cond-> season (not (initialized? season)) (generate-grid))]
    (if (race/finished? season grid-size)
      (cond-> (end new-season)
        (seq races) (update ::races rest)
        (seq races) (merge (first races)))
      new-season)))

(defn finished? [{::keys [n-races races results]}]
  (and (not (seq races))
       (= n-races (count results))))
