(ns lfs-career.core
  (:require [lfs-career.career :as career]
            [lfs-career.lfs :as lfs :refer [->lfs!]]
            [lfs-career.season :as season]
            [lfs-career.race :as race]
            [clj-insim.core :as clj-insim]))

(declare lfs-client)

(defonce state (atom nil))

(defn start-season! [season-key]
  (let [new-state (swap! state career/start-season season-key)]
    (->lfs! lfs-client (lfs/prepare-season-commands new-state))))

(defn next-race! []
  (let [new-state (swap! state season/next-race)]
    (->lfs!
     lfs-client
     (concat
      (lfs/prepare-race-commands new-state)
      (lfs/prepare-grid-commands new-state)))))

(comment

  (reset!
   state
   (career/make
    {:seasons [(season/make {:key :fbm-sprint
                             :cars #{"FBM"}
                             :races [(race/make {:track "BL1"})
                                     (race/make {:track "SO1R"})]
                             :unlocks {::career/unlocked-seasons #{:fbm-endurance}}})
               (season/make {:key :fbm-endurance
                             :cars #{"FBM"}
                             :races [(race/make {:track "BL1R"})
                                     (race/make {:track "FE3"})]
                             :unlocks {::career/unlocked-cars #{"FOX"}}})]
     :unlocked-cars #{"FBM"}
     :unlocked-seasons #{:fbm-sprint}}))

  (def lfs-client (clj-insim/client))

  (start-season! :fbm-sprint)
  (next-race!)

  (clj-insim/stop! lfs-client)

)
