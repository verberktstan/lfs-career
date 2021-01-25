(ns lfs-career.tracks)

(def ALL
  (->> (for [[env n] {"BL" 4 "SO" 6 "FE" 6 "AU" 4 "KY" 3 "WE" 5 "AS" 7 "RO" 11}]
         (for [x (range 1 (inc n))
               y [nil "R"]]
           (str env x y)))
       (flatten)
       (set)))
