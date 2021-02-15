(ns lfs-career.race
  (:require [lfs-career.tracks :as tracks]
            [lfs-career.result :as result]
            [lfs-career.utils :as u]
            [clojure.spec.alpha :as s]))

(s/def ::laps pos-int?) ;; Race length in laps
(s/def ::qual nat-int?) ;; Qualify length in minutes
(s/def ::track tracks/ALL) ;; Track code
(s/def ::results (s/nilable (s/coll-of ::result/model))) ;; Collection of race results

(s/def ::model
  (s/keys :req [::laps ::qual ::track ::results]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public functions

(defn make [{:keys [track laps qual]}]
  {:post [(u/validate ::model %)]}
  {::laps (or laps 3)
   ::qual (or qual 0)
   ::results nil
   ::track track})

(defn register-result [race result]
  (update race ::results conj result))

(defn finished?
  "Returns true if the number of registered results exceed the threshold (2/3 of
   the grid-size)"
  [{::keys [results]} grid-size]
  (let [threshold (-> grid-size (/ 1.5) int inc)]
    (>= (count results) threshold)))

(defn prepare [{::keys [track qual laps] :as season}
               sta]
  (concat
   (when-not (u/no-race-in-progress? sta) [["/end"]])
   (when-not (#{track} (:track sta)) [["/track" track]])
   [["/qual" qual]
    ["/laps" laps]]))
