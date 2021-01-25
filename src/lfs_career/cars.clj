(ns lfs-career.cars
  (:require [clojure.set :as set]))

(def ALL
  (set/union
   #{"UF1" "LX4"} ;; Unclassified
   #{"XFG" "XRG"} ;; FR
   #{"RB4" "FXO" "XRT"} ;; TBO
   #{"LX6" "RAC" "FZ5"} ;; LRF
   #{"UFR" "XFR"} ;; FWD GTR
   #{"FXR" "XRR" "FZR"} ;; GTR
   #{"MRT" "FBM" "FOX" "FO8" "BF1"} ;; S-S
))
