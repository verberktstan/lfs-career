(ns lfs-career.lfs
  (:require [lfs-career.grid :as grid]
            [lfs-career.race :as race]
            [lfs-career.season :as season]
            [clj-insim.packets :as packets]
            [clojure.string :as str]))

(defn prepare-race-commands [{::race/keys [laps qual track]}]
  ["loading race.."
   "/clear"
   (str "Loading track " track "..")
   (str "/track " track)
   {:sleep 4000}
   (str "Finished loading track " track)
   (str "/qual " qual)
   (str "/laps " laps)
   "Race loaded!"])

(defn prepare-grid-commands [{::season/keys [cars grid]}]
  (concat
   ["Loading grid.."]
   (mapcat
    (fn [ai car]
      [(str "/car " car)
       (str "/ai " ai)
       {:sleep 150}])
    grid (cycle cars))
   ["Grid loaded!"]))

(defn prepare-season-commands [{::season/keys [cars]}]
  (concat
   ["Loading season.."
    (str "/cars " (str/join "+" cars))]
   #_(prepare-race-commands season)
   #_(prepare-grid-commands grid (cycle cars))
   ["Season loaded!"]))

(defn ->lfs!
  "Sends all strings in coll to LFS as an mst command.
   If the item in coll is {:sleep 4000}, the thread will sleep for 4 seconds
   before continuing."
  [{:keys [enqueue!]} coll]
  (doseq [{:keys [sleep] :as s} coll]
    (cond
      (string? s) (enqueue! (packets/mst s))
      sleep (Thread/sleep sleep)
      :else (throw (ex-info "Cannot send to LFS!" s)))))
