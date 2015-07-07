(ns proto.db.goods-prices-test
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
    (tests)
    (reset-db!)))

(use-fixtures :each
  (fn [tests]
    (create-sample-shops!)
    (create-sample-good!)
    (tests)
    (reset-db!)))

(deftest add-price-test
  (testing "Adds a price."
    (let [shop (shops/find-by-name "Shop 1")
          good (goods/find-by-barcode "12345")
          item-price (prices/save! good shop 45)]
      (is (some? (:_id item-price))))))

(deftest add-price-for-multiple-shops-test
  (testing "Adding price for an item in more than one store."
    (let [shop1 (shops/find-by-name "Shop 1")
          shop2 (shops/find-by-name "Shop 2")
          good (goods/find-by-barcode "12345")
          price-at-shop1 (prices/save! good shop1 45)
          price-at-shop2 (prices/save! good shop2 50)]
      (is (= 50 (:price price-at-shop2)))
      (is (= (:_id shop2) (:_id (:shop price-at-shop2)))))))

(deftest update-price-test
  (testing "Updates a price for a good."
    (let [shop (shops/find-by-name "Shop 1")
          good (goods/find-by-barcode "12345")]
      (prices/save! good shop 45)
      (prices/save! good shop 50)
      (is (= 50 (:price (prices/find-current-price-at (:_id good) "Shop 1")))))))

(deftest search-price-for-item-in-store-test
  (testing "Searches for price of item at a store."
    (let [shop (shops/find-by-name "Shop 1")
          placebo-shop (shops/find-by-name "Placebo")
          good (goods/find-by-barcode "12345")]
      (prices/save! good shop 50)
      (is (nil? (:price (prices/find-current-price-at (:_id good) "Placebo"))))
      (is (= 50 (:price (prices/find-current-price-by-barcode-at "12345" "Shop 1")))))))


