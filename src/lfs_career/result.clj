(ns lfs-career.result
  (:require [lfs-career.cars :as cars]
            [lfs-career.utils :as u]
            [clojure.spec.alpha :as s]))

(s/def ::car-name cars/ALL) ;; Name of the driven car
(s/def ::player-name string?) ;; Name of the player
(s/def ::player-type #{:ai 0}) ;; 0 = player
(s/def ::result-num nat-int?) ;; 0 indexed race result

(s/def ::model
  (s/keys :req [::car-name ::player-name ::player-type ::result-num]))

(defn make [{:keys [player-name result-num player-type car-name]}]
  {:post [(u/validate ::model %)]}
  {::car-name car-name
   ::player-name player-name
   ::player-type player-type
   ::result-num result-num})
