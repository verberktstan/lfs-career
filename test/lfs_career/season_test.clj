(ns lfs-career.season-test
  (:require [lfs-career.season :as season]
            [lfs-career.race :as race]
            [lfs-career.test-helpers :refer [is-thrown-ex?]]
            [clojure.test :refer [deftest testing is]]))

(def TEST_SEASON (season/make {:key :test-season
                               :races [(race/make {:track "BL1"})
                                       (race/make {:track "FE1"})]}))
(def WITH_RACE_RESULTS (-> TEST_SEASON
                                  (season/next-race)
                                  (update ::race/results conj {:player-name "AI 1"
                                                               :result-num 0})))

(deftest initialized?-test
  (testing "a season is not initialized by default"
    (is (not (#'season/initialized? TEST_SEASON))))
  (testing "generate-grid returns a initialized season"
    (is (#'season/initialized? (#'season/generate-grid TEST_SEASON)))))

(deftest next-race-test
  (testing "next-race"
    (testing "throws an exception if no races are left"
      (is-thrown-ex? '(season/next-race (assoc TEST_SEASON ::season/races []))))
    (testing "initializes the season if not yet done"
      (is (not (#'season/initialized? TEST_SEASON)))
      (is (-> TEST_SEASON season/next-race (#'season/initialized?))))
    (testing "moves race results data to season results"
      (is (= [[{:player-name "AI 1" :result-num 0}]]
             (-> WITH_RACE_RESULTS
                 (season/next-race)
                 (::season/results)))))
    (testing "sets next in season/races as the current race"
      (let [next-race (-> TEST_SEASON ::season/races first)]
        (is (= (::race/track next-race)
               (::race/track (season/next-race TEST_SEASON))))))))

(deftest end-test
  (testing "end"
    (testing "moves race results data to season results"
      (is (= [[{:player-name "AI 1" :result-num 0}]]
             (-> WITH_RACE_RESULTS
                 (season/end)
                 (::season/results)))))))
