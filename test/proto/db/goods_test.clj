(ns proto.db.goods-test
  (:require
   [clojure.test :refer :all]
   [proto.db.goods :as goods]
   [proto.db.shops :as shops]
   [proto.db.goods-prices :as goods-prices]
   [proto.db.core :refer [db authenticate-db!]]
   [monger.collection :as mc]
   [proto.db.fixtures :refer :all]))

(use-fixtures :once
  (fn [tests]
    (authenticate-db!)
    (tests)))

(use-fixtures :each
  (fn [tests]
    (tests)
    (reset-db!)))

(deftest finds-good-by-barcode-test
  (testing "Can find a good by its barcode."
    (let [barcode "00894334"]
      (goods/create! {:barcode barcode :name "A Good With A Barcode" :description "Testing barcode search"})
      (is (= barcode (:barcode (goods/find-by-barcode barcode)))))))

(deftest barcodes-are-unique
  (testing "Does not create goods with duplicate barcodes."
    (let [barcode "1111111"
          good (goods/create! {:barcode barcode :name "First Good" :description "The First Good"})
          duplicate-good (goods/create! {:barcode barcode :name "Duplicate Good" :description "Good with duplicate barcode."})
          all-goods (goods/all 0)
          goods-with-barcode (filterv (fn [it] (= (:barcode it) barcode)) all-goods)]
      (is (= {} duplicate-good))
      (is (= 1 (count goods-with-barcode)))
      (is (= good (first goods-with-barcode))))))

