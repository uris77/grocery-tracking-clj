(ns proto.db.goods-search-test
  (:require
   [clojure.test :refer :all]
   [proto.db.goods :as goods]
   [proto.db.shops :as shops]
   [proto.db.goods-prices :as goods-prices]
   [proto.db.goods-search :as goods-search]
   [proto.db.core :refer [db authenticate-db!]]
   [monger.collection :as mc]
   [proto.db.fixtures :refer :all]))

(use-fixtures :once
  (fn [tests]
    (authenticate-db!)
    (tests)))

(use-fixtures :each
  (fn [tests]
    (create-sample-shops!)
    (tests)
    (reset-db!)))

(deftest find-prices-at-shops-nearby-test
  (testing "Finds prices for a good in all nearby shops."
    (let [barcode 111111
          peanut-butter (goods/create! {:barcode barcode :name "Peanut Butter" :description "Food"})
          fancisco (shops/find-by-name "Fancisco")
          current-location {:lon -88.76979229999999 :lat 17.2556004}]
      (goods-prices/save! peanut-butter fancisco 10.25)
      (let [found-prices (goods-search/find-prices-for-good-near-location peanut-butter current-location)]
        (is (= 3 (count found-prices)))
        (is (= 1 
               (count 
                (filter 
                 (fn [it] (not (nil? (:price it)))) 
                 found-prices))))))))
