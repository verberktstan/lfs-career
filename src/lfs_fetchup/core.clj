(ns lfs-fetchup.core
  (:require [lfs-fetchup.alter :refer [alter-setup]]
            [hickory.core :as hickory]
            [hickory.select :as s]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- track->web-code
  "Returns the web code for a game track code
   `BL1` => `BL00`
   `BL2R` => `BL11`"
  [track-code]
  (when-let [matches (seq (rest (re-matches #"([A-Z]{2})([0-9]+)(R?)" track-code)))]
    (let [[env l r] matches
          layout (edn/read-string l)
          rev? (#{"R"} r)]
      (str env (dec layout) (if rev? 1 0)))))

(defn- make-uri [car track-code]
  (str
   "https://www.lfs.net/files/setups/"
   car
   "/"
   (track->web-code track-code)))

(defn- read-doc [uri]
  (-> (slurp uri) hickory/parse hickory/as-hickory))

(defn- get-downloads [setup-tr]
  (-> (s/select (s/tag :td) setup-tr)
      (get 4)
      :content
      first
      edn/read-string))

(defn- get-class [setup-tr]
  (-> (s/select (s/tag :td) setup-tr)
      (get 2)
      :content
      first
      str/lower-case))

(defn- get-attachment [setup-tr]
  (-> (s/select (s/tag :td) setup-tr)
      (get 0)
      :content
      first
      :attrs
      :href))

(defn- parse-setup [setup-tr]
  {:n-downloads (get-downloads setup-tr)
   :class (get-class setup-tr)
   :attachment (get-attachment setup-tr)})

(defn- get-setups [car track-code]
  (let [doc (read-doc (make-uri car track-code))
        setup-file-list (s/select (s/descendant (s/class "filesSetupList")) doc)
        table (s/select (s/tag :tbody) (first setup-file-list))]
    (->> (s/select (s/tag :tr) (first table))
         (map parse-setup))))
  
(defn- rank-setups [classification setups]
  (let [classes (set (map :class setups))]
    (cond->> setups
      (contains? classes classification) (filter #(#{classification} (:class %))))))

(defn- get-download-url [{:keys [attachment]}]
  (str "https://www.lfs.net" attachment))

(defn- filename [output-dir car track-code & [postfix]]
  (str output-dir "/" car "_" track-code (when postfix postfix) ".set"))

(def ^:private VARIANTS [{:postfix "_ROOKIE" :restriction 1 :mass 30}])

(defn get-a-setup
  ([car track-code output-dir]
   (get-a-setup car track-code output-dir {:classification "hotlap / qualify"}))
  ([car track-code output-dir {:keys [classification variants]}]
   (let [setups (->> (get-setups car track-code)
                     (rank-setups (or classification "hotlap / qualify")))]
     (if (seq setups)
       (let [download-url (-> (first setups) get-download-url)
             file (filename output-dir car track-code)]
         (with-open [in (io/input-stream download-url)
                     out (io/output-stream file)]
           (io/copy in out))
         (when-let [variants (seq (or variants VARIANTS))]
           (doseq [{:keys [postfix] :as variant} variants]
             (with-open [in (io/input-stream file)]
               (alter-setup in (filename output-dir car track-code postfix) variant))))
         (println "Done! =>" file))
       (println "Can't find setup for this car/track combo!")))))

(defn- get-config []
  (edn/read-string (slurp "config.edn")))

(defn -main [car track-code]
  (if (and car track-code)
    (get-a-setup car track-code (:setup-dir (get-config)))
    (throw (Exception. "No arguments!"))))

(comment
  (get-a-setup "XRT" "BL1" (:output-dir (get-config)))
)
