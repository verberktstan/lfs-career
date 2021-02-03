(ns lfs-career.channels
  (:require [clojure.core.async :as async]))

;; This is a registery of channels

(defonce ^:private channels (atom nil))

(defn unregister-chan [k]
  (when-let [chan (get @channels k)]
    (async/close! chan)
    (swap! channels dissoc k)
    chan))

(defn register-chan
  "Register a new chan in the channels map, and return it."
  ([k]
   (register-chan k nil))
  ([k xform]
   (unregister-chan k) ;; Just to be sure, close/unregister if chan already exists
   (-> channels
       (swap! assoc k (if xform (async/chan xform) (async/chan)))
       (get k))))

(defn put!! [k v]
  (when-let [chan (get @channels k)]
    (async/>!! chan v)))
