(ns lfs-career.core
  (:require [lfs-career.career :as career]
            [lfs-career.lfs :as lfs :refer [->lfs!]]
            [lfs-career.season :as season]
            [lfs-career.race :as race]
            [clj-insim.core :as clj-insim]
            [clj-insim.packets :as packets]
            [clj-insim.models.packet :as packet]
            [clojure.set :as set]
            [clojure.string :as str]))

(declare lfs-client)

(defonce state (atom nil))

(defn start-season! [season-key]
  (let [new-state (swap! state career/start-season season-key)]
    (->lfs!
     lfs-client
     (concat
      [(str "Welcome to the season: " (name season-key))]
      (lfs/prepare-season-commands new-state)))))

(defn register-result! [race-result]
  (swap! state update ::race/results conj race-result))

(defn next-race! []
  (let [new-state (swap! state season/next-race)]
    (->lfs!
     lfs-client
     (concat
      [(season/starting-message new-state)]
      (lfs/prepare-race-commands new-state)
      (lfs/prepare-grid-commands new-state)))))

(defn end-season! []
  (let [new-state (swap! state career/end-season)]
    (->lfs!
     lfs-client
     [(str "Available seasons; " (str/join ", " (::career/unlocked-seasons new-state)))
      (str "Available cars; " (str/join ", " (::career/unlocked-cars new-state)))])))

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

(defn- parse-mso-command [{:keys [user-type text-start message]}]
  (when (#{2} user-type)
    (str/split (subs message text-start) #" ")))

(def ^:private help-info
  ["Type `!unlocked` to show all the unlocked cars and seasons"
   "Type `!start-season fbm-sprint` to start the FBM sprint season"
   "Type `!next-race` to load the track and grid for next race"
   "Type `!end-season` to end the season and unlock cars and seasons"])

(defn- dispatch-command [command args]
  (case command
    "!help" (map packets/mst help-info)
    "!unlocked" (map packets/mst (career/list-unlocked @state))
    "!start-season" (future (start-season! (-> args first keyword)))
    "!next-race" (future (next-race!))
    "!end-season" (future (end-season!))
    (throw (ex-info "Unknown command!" {}))))

(defmethod dispatch :mso [{::packet/keys [body header]}]
  (when-let [[command & args] (parse-mso-command body)]
    (try
      (dispatch-command command args)
      (catch Exception e (packets/mst (.getMessage e))))))

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
