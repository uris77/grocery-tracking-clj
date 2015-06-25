(ns proto.db.goods-search
  (:require
   [monger.core :as mg]
   [monger.collection :as coll]
   [monger.query :as query]
   [proto.db.core :refer [db conn]]
   [proto.db.goods :as goods]
   [proto.db.shops :as shops]
   [proto.db.goods-prices :as goods-prices]))

(defn find-prices-for-good-near-location
  "Finds the prices for a good within a 5 mile radius of
  the given location."
  [good location]
  (let [shops-nearby (shops/find-within [(:lon location) (:lat location)] 5)]
    (letfn [(get-price [shop]
              (let [price (goods-prices/find-current-price-at (:_id good) (:name shop))]
                (if (:price price) 
                  price
                  {:shop shop :name (:name shop)})))]
      (map get-price shops-nearby))))
