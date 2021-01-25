(ns lfs-career.core
  (:require [lfs-career.lfs :refer [->lfs!]]
            [lfs-career.race :as race]
            [clj-insim.core :as clj-insim]))

;; Single player career

;; Season
;;  - Events

;; Event
;;  - (Multiple) races

;; Race
;;  - Track
;;  - Car
;;  - 11 ai's
;;  - Qualification minutes
;;  - Race laps

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Configuration of races, a collection of races where the :race/track is
;; specified (at least)

(def races
  [{::race/track "FE1" ::race/laps 5 ::race/qual 10}
   {::race/track "FE2" ::race/laps 5 ::race/qual 15}])

(comment

  (def lfs-client (clj-insim/client))

  (->lfs! lfs-client (race/prepare (get races 0)))

  (clj-insim/stop! lfs-client)

)
