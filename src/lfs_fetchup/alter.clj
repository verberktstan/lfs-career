(ns lfs-fetchup.alter
  (:require [marshal.core :as m]
            [clojure.java.io :as io]))

(def ^:private codec (m/array m/ubyte 132))

(defn- set-hcmass [mass v]
  (assoc v 22 mass))

(defn- set-hcrestriction [restriction v]
  (assoc v 23 restriction))

(defn alter-setup
  "Read in-file, alter mass/restriction bytes and write to out-file"
  [input-stream out-file {:keys [mass restriction]}]
  (with-open [out (io/output-stream out-file)]
    (let [data (m/read input-stream codec)
          new-data (->> (vec data)
                        (set-hcmass (or mass 0))
                        (set-hcrestriction (or restriction 0)))]
      (m/write out codec new-data))))
