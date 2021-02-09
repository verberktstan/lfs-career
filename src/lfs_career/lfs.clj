(ns lfs-career.lfs
  (:require [clj-insim.core :as clj-insim]
            [clj-insim.packets :as packets]
            [clj-insim.models.packet :as packet]
            [clojure.core.async :as a]
            [clojure.string :as str]))

(def ^:private confirm-chan (atom nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Configuration of commands that need to park the current thread and await a
;; response via confirm-chan. Keys are the parking commands and vals are the
;; confirmations

(def ^:private cmd-responses
  {"/ai"    :ai
   "/clear" :clr
   "/end"   :ren
   "/track" :axc})

(def ^:private parking-commands (set (keys cmd-responses)))
(def ^:private confirmation (set (vals cmd-responses)))

(defn ->lfs! [{:keys [enqueue!]} commands]
  (let [chan (reset! confirm-chan (a/chan))]
    (a/go-loop [coll commands]
      (if-not (seq coll)
        (a/close! chan)
        (let [[cmd arg :as x] (first coll)]
          (enqueue! (packets/mst (str/join " " x)))
          (when-not (parking-commands cmd) ; Check if this is (not) a blocking command
            (a/put! chan :next)) ; If this is the case, automaticly confirm, so we can continue
          (println "Confirmation:" (a/<! chan)) ; Otherwise, thread is parked untill we get a confirmation
          (recur (rest coll)))))))

(defmulti dispatch clj-insim/packet-type)

(defmethod dispatch :default [p] p)

(defmethod dispatch :tiny [{::packet/keys [header] :as packet}]
  (when (and @confirm-chan
             (-> header :data confirmation)) ; When AutoX Cleared (axc), CLear Race (clr) or Race ENd (ren)
    (a/put! @confirm-chan :track))
  packet)

(defmethod dispatch :npl [{::packet/keys [header body] :as packet}]
  (when (and @confirm-chan
             (-> body :num-player #{0} not) ; When not a join request
             (-> header :request-info #{0}) ;; and not a response
             (-> body :player-type confirmation)) ;; and new player is AI (one of the confirmation keys is :ai)
    (a/put! @confirm-chan :ai))
  packet)

#_(defonce ^:private race-in-progress (atom :no-race))

#_(defmethod dispatch :sta [{::packet/keys [body] :as packet}]
  (when (and @confirm-chan
             (-> @race-in-progress #{:no-race} not)
             (-> body :race-in-progress #{:no-race})
             (not (contains? (:iss-state-flags body) :front-end)))
    (a/put! @confirm-chan :no-race-in-progress))
  (reset! race-in-progress (:race-in-progress body))
  packet)
