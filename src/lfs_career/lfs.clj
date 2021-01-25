(ns lfs-career.lfs
  (:require [clj-insim.packets :as packets]))

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
