(ns lfs-career.core
  (:require [lfs-career.lfs :refer [->lfs!]]
            [lfs-career.season :as season]
            [lfs-career.race :as race]
            [clj-insim.core :as clj-insim]))

(def season
  (season/make {:cars #{"FBM"}
                :grid-size 12
                :races [(race/make {:track "BL1" :laps 5})
                        (race/make {:track "SO1" :laps 5})]}))

(comment

  (def lfs-client (clj-insim/client))

  (let [season (-> season season/initialize season/next-race)]
    (->lfs! lfs-client (season/prepare season)))

  (clj-insim/stop! lfs-client)

)
