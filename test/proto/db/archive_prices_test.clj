(ns proto.db.archive-prices-test
  (:require
   [clojure.test :refer :all]
   [proto.db.core :refer [db conn authenticate-db! close! add-full-text!]]
   [proto.db.shops :as shops]
   [proto.db.goods :as goods]
   [proto.db.goods-prices :as prices]
   [proto.db.fixtures :refer :all]
   [monger.collection :as mc]))

(use-fixtures :once
  (fn [tests]
    (authenticate-db!)
    (tests)))

(use-fixtures :each
  (fn [tests]
    (create-sample-shops!)
    (create-sample-good!)
    (tests)
    (reset-db!)))

(deftest archives-price-test
  (testing "Archives price test when the price is updated."
    (let [shop (shops/find-by-name "Shop 1")
          good (goods/find-by-barcode 12345)]
      (prices/save! good shop 45)
      (let [saved (prices/save! good shop 50)
            history (:history saved)]
        (let [history (prices/history-for good)]
          (is (= good (:good history))))
        ))))

(deftest views-price-history-for-good-at-a-shop-test
  (testing "Views price history for a good at a shop."
    (let [shop (shops/find-by-name "Shop 2")
          good (goods/find-by-barcode 12345)]
      (prices/save! good shop 35)
      (prices/save! good shop 50)
      (prices/save! good shop 51)
      (let [history (prices/history-for-good-at good shop)]
        (is (= 2 (count (:prices history))))))))

