(ns lfs-career.core
  (:require [lfs-career.career :as career]
            [lfs-career.lfs :as lfs :refer [->lfs!]]
            [lfs-career.season :as season]
            [lfs-career.race :as race]
            [clj-insim.core :as clj-insim]
            [clj-insim.models.packet :as packet]
            [clojure.set :as set]
            [clojure.string :as str]))

(declare lfs-client)

(defonce state (atom nil))

(defn start-season! [season-key]
  (let [new-state (swap! state career/start-season season-key)]
    (->lfs! lfs-client (lfs/prepare-season-commands new-state))))

(defn register-result! [race-result]
  (swap! state update ::race/results conj race-result))

(defn next-race! []
  (let [new-state (swap! state season/next-race)]
    (->lfs!
     lfs-client
     (concat
      (lfs/prepare-race-commands new-state)
      (lfs/prepare-grid-commands new-state)))))

(defn end-season! []
  (let [prev-seasons (::career/unlocked-seasons @state)
        prev-cars (::career/unlocked-cars @state)
        new-state (swap! state career/end-season)]
    (->lfs!
     lfs-client
     [(str "You've unlocked seasons; "
           (str/join ", " (set/difference prev-seasons (::career/unlocked-seasons new-state))))
      (str "You've unlocked cars; "
           (str/join ", " (set/difference prev-cars (::career/unlocked-cars new-state))))])))

(defmulti dispatch clj-insim/packet-type)

(defmethod dispatch :default [_] nil)

(defn- combine-result [packet-body player]
  (merge
   (select-keys packet-body [:player-name :result-num])
   (select-keys player [:player-type :car-name])))

(defmethod dispatch :res [{::packet/keys [body header]}]
  (when (contains? (:confirmation-flags body) :confirmed)
    (let [player-id (:data header)]
      (register-result! (combine-result body (clj-insim/get-player player-id))))))

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
                                     (race/make {:track "FE2"})]
                             :unlocks {::career/unlocked-cars #{"FOX"}}})
               (season/make {:key :fox-sprint
                             :cars #{"FOX"}
                             :races [(race/make {:track "BL1"})
                                     (race/make {:track "FE3"})]
                             :unlocks {::career/unlocked-seasons #{:fox-endurance}}})
               (season/make {:key :fox-endurance
                             :cars #{"FOX"}
                             :races [(race/make {:track "AS4"})
                                     (race/make {:track "RO1"})]})]
     :unlocked-cars #{"FBM"}
     :unlocked-seasons #{:fbm-sprint}}))

  (def lfs-client (clj-insim/client dispatch))

  (start-season! :fbm-sprint)
  (next-race!)
  (end-season!)

  @state

  (clj-insim/stop! lfs-client)

)
