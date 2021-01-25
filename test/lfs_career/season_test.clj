(ns lfs-career.season-test
  (:require [lfs-career.season :as season]
            [lfs-career.race :as race]
            [clojure.test :refer [deftest testing is]]))

(def TEST_SEASON (season/make {:key :test-season}))
(def SEASON_WITH_RESULT (season/register-result
                         TEST_SEASON
                         {:player-name "AI 1"
                          :result-num 0}))

(deftest register-result-test
  (testing "register-result"
    (testing "registers one result"
      (is (= 1 (-> SEASON_WITH_RESULT ::season/result count))))
    (testing "registers another result"
      (is (= 2 (-> SEASON_WITH_RESULT
                   (season/register-result {:player-name "AI 2"
                                            :result-num 0})
                   ::season/result count))))))

(deftest next-race-test
  (testing "next-race"
    (testing "throws an exception if not result for current race"
      (is (thrown? Exception (season/next-race TEST_SEASON))))
    (let [season (season/next-race SEASON_WITH_RESULT)]
      (testing "stores result"
        (is (= 1 (-> season ::season/results count))))
      (testing "picks the next race"
        (is (= (-> TEST_SEASON ::season/races count dec)
               (-> season ::season/races count))))
      (testing "throws an exception if no more races left"
        (is (thrown? Exception (season/next-race season)))))))
