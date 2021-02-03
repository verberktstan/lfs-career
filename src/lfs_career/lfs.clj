(ns lfs-career.lfs
  (:require [clj-insim.core :as clj-insim]
            [clj-insim.packets :as packets]
            [clj-insim.models.packet :as packet]
            [clojure.core.async :as a]
            [clojure.string :as str]))

(def ^:private confirm-chan (atom nil))

(defn ->lfs! [{:keys [enqueue!]} commands]
  (let [chan (reset! confirm-chan (a/chan))]
    (a/go-loop [coll commands]
      (if-not (seq coll)
        (a/close! chan)
        (let [[cmd arg :as x] (first coll)]
          (enqueue! (packets/mst (str/join " " x)))
          (when-not (#{"/track" "/ai" "/end"} cmd) ; Check if this is a blocking command
            (a/put! chan :next)) ; Automaticly confirm, so we can continue
          (println "Confirmation:" (a/<! chan)) ; Check if confirmed
          (recur (rest coll)))))))

(defmulti dispatch clj-insim/packet-type)

(defmethod dispatch :default [p] p)

(defmethod dispatch :tiny [{::packet/keys [header] :as packet}]
  (when (and @confirm-chan
             (-> header :data #{:axc})) ; When AutoX cleared
    (a/put! @confirm-chan :track))
  packet)

(defmethod dispatch :npl [{::packet/keys [header body] :as packet}]
  (when (and @confirm-chan
             (-> body :num-player #{0} not) ; When not a join request
             (-> header :request-info #{0}) ;; and not a response
             (-> body :player-type #{:ai})) ;; and new player is AI
    (a/put! @confirm-chan :ai))
  packet)

(defonce ^:private race-in-progress (atom :no-race))
(defmethod dispatch :sta [{::packet/keys [body] :as packet}]
  (when (and @confirm-chan
             (-> @race-in-progress #{:no-race} not)
             (-> body :race-in-progress #{:no-race})
             (not (contains? (:iss-state-flags body) :front-end)))
    (a/put! @confirm-chan :no-race-in-progress))
  (reset! race-in-progress (:race-in-progress body))
  packet)
