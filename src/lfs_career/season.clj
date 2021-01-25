(ns lfs-career.season
  (:require [lfs-career.grid :as grid]
            [lfs-career.race :as race]
            [lfs-career.utils :as u]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(s/def ::cars (s/coll-of #{"FBM" "FOX" "FO8" "BF1"} :kind set?))
(s/def ::grid ::grid/model)
(s/def ::grid-size pos-int?)
(s/def ::race ::race/model)
(s/def ::races (s/coll-of ::race/model))

(s/def ::model
  (s/keys :req [::cars ::grid-size ::races]
          :opt [::grid ::race]))

(defn make [{:keys [cars grid-size races]}]
  {:post [(u/validate ::model %)]}
  {::cars cars ::grid-size (or grid-size 20) ::races races})

(defn- initialized? [season]
  (::grid season))

(defn initialize [{::keys [grid-size] :as season}]
  (cond-> season
    (not (initialized? season))
    (assoc ::grid (grid/generate (dec grid-size)))))

(defn next-race [{::keys [races] :as season}]
  (when-not (seq races)
    (throw (ex-info "No more races left in season!" season)))
  (u/check initialized? season "Season is not initialized!")
  (-> season
      (update ::races rest)
      (merge (first races))))

(defn- prepare-grid [{::keys [cars grid]}]
  (grid/prepare grid (cycle cars)))

(defn prepare [{::keys [cars] :as season}]
  (u/validate ::model season)
  (u/check initialized? season "Season is not initialized!")
  (concat
   ["Loading next race.."
    (str "/cars " (str/join "+" cars))]
   (race/prepare season)
   ["Loading grid.."]
   (prepare-grid season)
   ["Grid loaded!"]))
