(ns lfs-career.component
  (:require [clj-insim.core :as clj-insim]
            [clj-insim.packets :as packets]))

(defonce ^:private lfs-client (atom nil))

(defn start!
  "Starts the InSim client, resetting the lfs-client atom."
  [dispatch-fn]
  (reset!
   lfs-client
   (clj-insim/client
    {:sleep-interval 50}
    (packets/insim-init {:is-flags #{:req-join}})
    dispatch-fn)))

(defn get-lfs-client []
  (let [lc @lfs-client]
    (when @(:running lc) lc)))

(defn stop!
  "Stops the InSim client."
  []
  (clj-insim/stop! @lfs-client))
