(ns lfs-career.core
  (:require [lfs-career.lfs :refer [->lfs!]]
            [lfs-career.season :as season]
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

(def seasons
  [(season/make {:cars #{"FBM"}
                 :races [(race/make {:track "BL1" :laps 5})
                         (race/make {:track "SO1" :laps 5})]})])

(comment

  (def lfs-client (clj-insim/client))

  (->lfs! lfs-client (race/prepare (get-in seasons [0 :season/races 0]))o)

  (clj-insim/stop! lfs-client)

)
