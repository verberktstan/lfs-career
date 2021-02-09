(ns lfs-career.core
  (:require [lfs-career.career :as career]
            [lfs-career.lfs :as lfs :refer [->lfs!]]
            [lfs-career.season :as season]
            [lfs-career.race :as race]
            [lfs-fetchup.core :as fetchup]
            [clj-insim.core :as clj-insim]
            [clj-insim.packets :as packets]
            [clj-insim.models.packet :as packet]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.core.async :as a]))

(declare lfs-client)

(defonce state (atom nil))

(defonce sta (atom nil))

(def ^:private header-line (packets/mst "-=#############=-"))

(defn- prepare-season [{::season/keys [cars key]}]
  [[(str "Welcome to the season: " (name key))]
   ["/cars" (str/join "+" cars)]])

(defn- start-season! [lfs-client season-key]
  (->lfs!
   lfs-client
   (prepare-season (swap! state career/start-season season-key))))

(defn- register-result! [race-result]
  (swap! state update ::race/results conj race-result))

(defn- get-config []
  (-> "config.edn" slurp edn/read-string))

(defn- fetch-setups [career]
  (doseq [car (::season/cars career)]
    (fetchup/get-a-setup
     car
     (::race/track career)
     (-> (get-config) :setup-dir)
     {:variants []})))

(defn- race-in-progress? [sta]
  (-> sta :race-in-progress #{:race}))

(defn- qualifying-in-progress? [sta]
  (-> sta :race-in-progress #{:qualifying}))

(defn- no-race-in-progress? [sta]
  (-> sta :race-in-progress #{:no-race}))

(defn- prepare-race [{::race/keys [track qual laps] :as season}
                     sta]
  (future (fetch-setups season))
  (concat
   (when-not (no-race-in-progress? sta) [["/end"]])
   (when-not (#{track} (:track sta)) [["/track" track]])
   [["/qual" qual]
    ["/laps" laps]]))

(defn- cars-and-ai [{::season/keys [cars grid]}]
  (->> (interleave (cycle cars) grid)
       (partition 2)))

(defn- prepare-grid [{::season/keys [cars grid] ::race/keys [track] :as season}]
  (concat
   [["/clear"]]
   (mapcat
    (fn [[car ai]]
      [["/car" car]
       ["/setup" track]
       ["/ai" ai]])
    (cars-and-ai season))))

(defn- prepare-next-race [sta season]
  (concat
   (prepare-race season sta)
   (prepare-grid season)))

(defn- next-race! [lfs-client]
  (->lfs!
   lfs-client
   (prepare-next-race @sta (swap! state season/next-race))))

(defn- prepare-end-season [{::career/keys [unlocked-seasons unlocked-cars]}]
  [[(str "Available seasons; " (str/join ", " unlocked-seasons))]
   [(str "Available cars; " (str/join ", " unlocked-cars))]])

(defn- end-season! [lfs-client]
  (->lfs!
   lfs-client
   (prepare-end-season (swap! state career/end-season))))

(defmulti dispatch clj-insim/packet-type)

(defmethod dispatch :default [_] nil)

(defn- combine-result [packet-body player]
  (merge
   (select-keys packet-body [:player-name :result-num])
   (select-keys player [:player-type :car-name])))

(defmethod dispatch :res [{::packet/keys [body header]}]
  (when (and
         (race-in-progress? @sta)
         (contains? (:confirmation-flags body) :confirmed))
    (let [player-id (:data header)]
      (register-result! (combine-result body (clj-insim/get-player player-id))))))

(defn- parse-mso-command [{:keys [user-type text-start message]}]
  (when (#{2} user-type)
    (str/split (subs message text-start) #" ")))

(defn- parse-season [data]
  (-> (update data :races (partial map race/make))
      (update :unlocks set/rename-keys {:seasons ::career/unlocked-seasons
                                        :cars ::career/unlocked-cars})
      (season/make)))

(defn- read-career [filename]
  (career/make
   (update
    (-> filename slurp edn/read-string)
    :seasons
    (partial map parse-season))))

(defn- available-files
  "Returns a map of filenames of .edn files (without extension) found in the
     requested directory, associated with their full path. Supply a directory
     (without trailing /, eg `careers`"
  [dir]
  (let [regex (re-pattern (str dir "/([a-z]+).edn"))]
    (->>
     (file-seq (io/file dir))
     (filter #(.isFile %))
     (map str)
     (map (partial re-matches regex))
     (into {})
     (set/map-invert))))

(defn- import-career! [s]
  (let [files (available-files "careers")
        path (get files s)]
    (if path
      (let [{::career/keys [key]} (reset! state (read-career path))]
        [header-line
         (packets/mst (str "Welcome to career " s))
         (packets/mst "!unlocked")])
      (packets/mst (str "Unknown career, try: " (str/join ", " (keys files)))))))

(def ^:private help-info
  ["Type `!career ow` to import the Open Wheel career"
   "Type `!unlocked` to show all the unlocked cars and seasons"
   "Type `!season fbm-sprint` to start the FBM sprint season"
   "Type `!race` to load the track and grid for next race"
   "Type `!end-season` to end the season and unlock cars and seasons"])

(defn- dispatch-command [command args]
  (case command
    "!help" (concat [header-line] (map packets/mst help-info))
    "!career" (import-career! (-> args first))
    "!unlocked" (map packets/mst (career/list-unlocked @state))
    "!season" (future (start-season! lfs-client (-> args first keyword)))
    "!race" (future (next-race! lfs-client))
    "!end-season" (future (end-season! lfs-client))
    (throw (ex-info "Unknown command!" {}))))

(defmethod dispatch :mso [{::packet/keys [body header]}]
  (when-let [[command & args] (parse-mso-command body)]
    (try
      (dispatch-command command args)
      (catch Exception e (packets/mst (doto (.getMessage e) println))))))

(defn- join-request? [{::packet/keys [body]}]
  (-> body :num-player #{0}))

(defmethod dispatch :npl [{::packet/keys [body] :as packet}]
  (when (join-request? packet)
    (let [{:keys [player-type car-name connection-id]} body
          cars (career/available-cars @state)]
      (cond
        (#{:ai} player-type)
        (packets/jrr connection-id :allow)

        (contains? cars car-name)
        (packets/jrr connection-id :allow)

        :else
        [(packets/jrr connection-id :disallow)
         (packets/mst (str car-name " is not allowed (allowed cars; " (str/join ", " cars) ")"))]))))

(defmethod dispatch :sta [{::packet/keys [header body]}]
  (reset! sta body))

(defmethod dispatch :ver [{::packet/keys [body]}]
  (let [{:keys [insim-version product version]} body]
    (println
     (format
      "Connected with LFS %s / %s (insim-version %s)"
      product version insim-version))))

(comment

  (import-career! "ow-career")

  (def lfs-client (clj-insim/client {:sleep-interval 50} (packets/insim-init {:is-flags #{:req-join}}) (comp dispatch lfs/dispatch)))

  (start-season! :fbm-sprint)
  (next-race!)
  (end-season!)
  
  @state
  @sta

  (clj-insim/stop! lfs-client)
  lfs-client
)
