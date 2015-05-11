(ns proto.db.goods-prices-test
  (:require
   [clojure.test :refer :all]
   [proto.db.core :refer [db conn authenticate-db! close! add-full-text!]]
   [proto.db.shops :as shops]
   [proto.db.goods :as goods]
   [proto.db.goods-prices :as prices]
   [monger.collection :as mc]))

(defn- create-sample-shops!
  []
  (shops/create! {:name "Shop 1" :latitude 90 :longitude 90})
  (shops/create! {:name "Shop 2" :latitude 90 :longitude 90})
  (shops/create! {:name "Placebo" :latitude 90 :longitude 90})
  (shops/create! {:name "The Clash" :latitude 90 :longitude 90})
  (shops/create! {:name "Soundgarden" :latitude 90 :longitude 90}))

(defn- create-sample-good!
  []
  (goods/create! {:barcode 12345 :name "Sample Item" :description "A Sample Item For Tests."}))

(defn- reset-db!
  []
  (mc/drop db shops/shops-coll)
  (mc/drop db goods/goods-coll)
  (mc/drop db prices/prices-coll))

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

(deftest add-price-test
  (testing "Adds a price."
    (let [shop (shops/find-by-name "Shop 1")
          good (goods/find-by-barcode "12345")
          item-price (prices/save! good shop 45)]
      (is (some? (:_id item-price))))))

(deftest update-price-test
  (testing "Updates a price for a good."
    (let [shop (shops/find-by-name "Shop 1")
          good (goods/find-by-barcode "12345")]
      (prices/save! good shop 45)
      (prices/save! good shop 50)
      (is (= 50 (:price (prices/find-current-price-at good "Shop 1")))))))