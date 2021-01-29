(ns lfs-career.career-test
  (:require [lfs-career.career :as career]
            [lfs-career.season :as season]
            [clojure.test :refer [deftest testing is]]))

(deftest start-season-test
  (testing "start-season"
    (testing "throws an exception if season is not available"
      (let [career (career/make {:seasons [(season/make {:key :test-season})
                                           (season/make {:key :another-season})]
                                 :unlocked-cars #{"FBM"}
                                 :unlocked-seasons #{:test-season}})]
        (is (thrown? Exception (career/start-season career :another-season)))))))
