(ns lfs-career.season
  (:require [lfs-career.race :as race]
            [lfs-career.utils :as u]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(s/def ::cars (s/coll-of #{"FBM" "FOX" "FO8" "BF1"} :kind set?))
(s/def ::grid (s/coll-of string?))
(s/def ::grid-size pos-int?)
(s/def ::race ::race/model)
(s/def ::races (s/coll-of ::race/model))

(s/def ::model
  (s/keys :req [::cars ::grid-size ::races]
          :opt [::grid ::race]))

(defn make [{:keys [cars grid-size races]}]
  {:post [(u/validate ::model %)]}
  {::cars cars ::grid-size (or grid-size 20) ::races races})

(defn- choose-ai [n]
  (let [n (min n 32)]
    (->> (range 1 33) (shuffle) (take n) (map (partial str "AI ")))))

(defn- initialized? [season]
  (::grid season))

(defn- check [pred x s]
  (when-not (pred x)
    (throw (ex-info s x))))

(defn initialize [{::keys [grid-size] :as season}]
  (cond-> season
    (not (initialized? season))
    (assoc ::grid (choose-ai (dec grid-size)))))

(defn next-race [{::keys [races] :as season}]
  (when-not (seq races)
    (throw (ex-info "No more races left in season!" season)))
  (check initialized? season "Season is not initialized!")
  (-> season
      (update ::races rest)
      (merge (first races))))

(defn- prepare-grid [{::keys [cars grid] :as season}]
  (let [cars (cycle cars)]
    (concat
     ["Loading grid.."]
     (mapcat
      (fn [car ai]
        [(str "/car " car)
         (str "/ai " ai)
         {:sleep 125}])
      cars grid))))

(defn prepare [{::keys [cars] :as season}]
  (u/validate ::model season)
  (check initialized? season "Season is not initialized!")
  (concat
   ["Loading next race.."
    (str "/cars " (str/join "+" cars))]
   (race/prepare season)
   (prepare-grid season)))
