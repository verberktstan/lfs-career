(ns lfs-career.season
  (:require [lfs-career.race :as race]
            [lfs-career.utils :as u]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(s/def ::cars (s/coll-of #{"FBM" "FOX" "FO8" "BF1"} :kind set?))
(s/def ::race ::race/model)
(s/def ::races (s/coll-of ::race/model))

(s/def ::model
  (s/keys :req [::cars ::races]
          :opt [::race]))

(defn make [{:keys [cars races]}]
  {:post [(u/validate ::model %)]}
  {::cars cars ::races races})

(defn next-race [{::keys [races] :as season}]
  (when-not (seq races)
    (throw (ex-info "No more races left in season!" season)))
  (-> season
      (update ::races rest)
      (merge (first races))))

(defn prepare [{::keys [cars] :as season}]
  (u/validate ::model season)
  (concat
   ["Loading next race.."
    (str "/cars " (str/join "+" cars))]
   (race/prepare season)))
