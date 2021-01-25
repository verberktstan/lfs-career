(ns lfs-career.race
  (:require [lfs-career.utils :as u]
            [clojure.spec.alpha :as s]))

(s/def ::laps pos-int?)
(s/def ::qual (s/nilable nat-int?))
(s/def ::track string?)

(s/def ::model
  (s/keys :req [::laps ::qual ::track]))

(defn prepare [{::keys [laps qual track] :as race}]
  (u/validate ::model race)
  ["/clear"
   (str "Loading track " track)
   (str "/track " track)
   {:sleep 4000}
   (str "Finished loading track " track)
   (str "/qual " (or qual 10))
   (str "/laps " (or laps 3))])

