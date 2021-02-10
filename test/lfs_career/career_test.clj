(ns lfs-career.career-test
  (:require [lfs-career.career :as career]
            [lfs-career.season :as season]
            [lfs-career.race :as race]
            [lfs-career.test-helpers :refer [is-thrown-ex?]]
            [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest testing is]]))

(def CAREER
  (career/make {:seasons [(season/make {:key :test-season
                                        :cars #{"UF1" "XFG" "XRG"}
                                        :unlocks {:lfs-career.career/unlocked-seasons #{:another-season}
                                                  :lfs-career.career/unlocked-cars #{"XRG"}}})
                          (season/make {:key :another-season})]
                :unlocked-cars #{"UF1" "XFG" "LX4"}
                :unlocked-seasons #{:test-season}}))

(def STARTED_TEST_SEASON
  (career/start-season CAREER :test-season))

(deftest career-season-test
  (testing "active-season"
    (testing "returns false if no season is active yet for this career"
      (is (not (#'career/active-season CAREER))))
    (testing "returns the career (a truthy value) if a season is started for this career"
      (is (#'career/active-season STARTED_TEST_SEASON)))))

(deftest start-season-test
  (testing "start-season"
    (testing "throws an exception if season is not unlocked"
      (is-thrown-ex? '(career/start-season CAREER :unkown-season)))
    (testing "throws an exception if another season is not unlocked"
      (is-thrown-ex? '(career/start-season STARTED_TEST_SEASON :another-season)))
    (testing "returns the career with the active season"
      (is (#'career/active-season STARTED_TEST_SEASON)))))

(deftest end-season-test
  (testing "end-season"
    (testing "throws an exception if no season is active (there's no season to end)"
      (is-thrown-ex? '(career/end-season CAREER)))
    (testing "throws an exception if season is not finished"
      (is-thrown-ex? '(-> CAREER (career/start-season :unkown-season) (career/end-season))))
    (let [career (-> STARTED_TEST_SEASON
                     (update ::race/results conj {:race "result"})
                     (season/next-race)
                     (season/end)
                     (career/end-season))]
      (testing "returns a valid career"
        (is (s/valid? ::career/model career)))
      (testing "returns a career that is not a active season"
        (is (not (#'career/active-season career))))
      (testing "unions the season season unlocks with the career/unlocked-seasons"
        (is (= #{:test-season :another-season}
               (::career/unlocked-seasons career))))
      (testing "unions the season car unlocks with the career/unlocked-cars"
        (is (= #{"UF1" "LX4" "XFG" "XRG"}
               (::career/unlocked-cars career)))))))

(deftest available-cars-test
  (testing "available-cars"
    (testing "returns an intersection of the career/unlocked-cars and season/cars"
      (is (= #{"UF1" "XFG"}
             (career/available-cars STARTED_TEST_SEASON))))))
