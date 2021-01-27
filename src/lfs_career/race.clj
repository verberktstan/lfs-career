(ns lfs-career.race
  (:require [lfs-career.tracks :as tracks]
            [lfs-career.utils :as u]
            [clojure.spec.alpha :as s]))

(s/def ::laps pos-int?) ;; Race length in laps
(s/def ::qual nat-int?) ;; Qualify length in minutes
(s/def ::track tracks/ALL) ;; Track code
(s/def ::results (s/nilable (s/coll-of map?))) ;; Collection of race result maps

(s/def ::model
  (s/keys :req [::laps ::qual ::track ::results]))

(defn make [{:keys [track laps qual]}]
  {:post [(u/validate ::model %)]}
  {::laps (or laps 3)
   ::qual (or qual 0)
   ::results nil
   ::track track})
