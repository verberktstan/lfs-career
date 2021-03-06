(ns lfs-career.core
  (:require [lfs-career.career :as career]
            [lfs-career.component :as component]
            [lfs-career.lfs :as lfs :refer [->lfs!]]
            [lfs-career.season :as season]
            [lfs-career.race :as race]
            [lfs-career.result :as result]
            [lfs-career.utils :as u]
            [lfs-fetchup.core :as fetchup]
            [clj-insim.core :as clj-insim]
            [clj-insim.packets :as packets]
            [clj-insim.models.packet :as packet]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.core.async :as a])
  (:gen-class))

(defonce state (atom nil))

(defonce sta (atom nil))

(defn- start-season! [lfs-client season-key]
  (let [season (try
                 (swap! state career/start-season season-key)
                 (catch Exception e
                   (->lfs! lfs-client [[(.getMessage e)]])))]
    (->lfs!
     lfs-client
     (season/prepare season))))

(defn- get-config []
  (-> "config.edn" slurp edn/read-string))

(defn- fetch-setups [career]
  (doseq [car (::season/cars career)]
    (fetchup/get-a-setup
     car
     (::race/track career)
     (-> (get-config) :setup-dir)
     {:variants []})))

(defn- prepare-next-race [sta season]
  (future (fetch-setups season))
  (concat
   (race/prepare season sta)
   (season/prepare-grid season)))

(defn- next-race! [lfs-client]
  (->lfs!
   lfs-client
   (prepare-next-race @sta (swap! state season/next-race))))

(defn- end-season! [lfs-client]
  (->lfs!
   lfs-client
   (career/prepare-end-season (swap! state career/end-season))))

(defmulti dispatch clj-insim/packet-type)

(defmethod dispatch :default [_] nil)

(defmethod dispatch :res [{::packet/keys [body header]}]
  (when (and (u/race-in-progress? @sta)
             (contains? (:confirmation-flags body) :confirmed))
    (let [player (-> header :data clj-insim/get-player)
          result (result/make (merge player body))]
      (swap! state race/register-result result))))

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
        [(packets/mst (str "Welcome to career " s))
         (packets/mst "!unlocked")])
      (packets/mst (str "Unknown career, try: " (str/join ", " (keys files)))))))

(defn- save! [lfs-client s]
  (spit (str "savegames/" s ".edn") @state)
  (packets/mst (str "Career saved as " s ", `!load " s "` to restore it")))

(defn- restore-save! [lfs-client s]
  (let [files (available-files "savegames")
        path (get files s)]
    (if path
      (let [career (reset! state (-> path slurp edn/read-string))]
        (->lfs!
         lfs-client
         (concat
          (season/prepare career)
          (prepare-next-race @sta career))))
      (->lfs!
       lfs-client
       [[(str "Savegame not found, try: " (str/join ", " (keys files)))]]))))

(def ^:private help-info
  ["Type `!career ow` to import the Open Wheel career"
   "Type `!unlocked` to show all the unlocked cars and seasons"
   "Type `!season fbm-sprint` to start the FBM sprint season"
   "Type `!race` to load the track and grid for next race"
   "Type `!end-season` to end the season and unlock cars and seasons"
   "Type `!save epic` to save the career (by the name of epic)"
   "Type `!load epic` to load the savegame that goes by the name epic"])

(defn- dispatch-command [lfs-client command args]
  (case command
    "!help" (map packets/mst help-info)
    "!career" (import-career! (-> args first))
    "!unlocked" (map packets/mst (career/list-unlocked @state))
    "!season" (future (start-season! lfs-client (-> args first keyword)))
    "!race" (future (next-race! lfs-client))
    "!end-season" (future (end-season! lfs-client))
    "!save" (save! lfs-client (-> args first))
    "!load" (future (restore-save! lfs-client (-> args first)))
    (throw (ex-info "Unknown command!" {}))))

(defmethod dispatch :mso [{::packet/keys [body header]}]
  (when-let [[command & args] (parse-mso-command body)]
    (try
      (dispatch-command (component/get-lfs-client) command args)
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
      product version insim-version))
    (packets/mst "!help")))

(defn -main []
  (let [setup-dir (:setup-dir (get-config))]
    (when-not (.isDirectory (io/file setup-dir))
      (println "Config error: Directory does not exist:" setup-dir)
      (println "Copy `config.edn.example` to `config.edn` and make sure :setup-dir points to your LFS/data/setups directory!")
      (System/exit 0)))
  (component/start! (comp dispatch lfs/dispatch))
  (println "Type exit, quit or stop to quit lfs-career")
  (loop [input nil]
    (if (#{"exit" "quit" "stop"} input)
      (do
        (component/stop!)
        (System/exit 0))
      (recur (read-line)))))

(comment
  (component/start! (comp dispatch lfs/dispatch))
  (component/stop!)
)
