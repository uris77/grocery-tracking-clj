(ns proto.db.fixtures
  (:require
   [proto.db.core :refer [db conn authenticate-db! close! add-full-text!]]
   [proto.db.shops :as shops]
   [proto.db.goods :as goods]
   [proto.db.goods-prices :as prices]
   [monger.collection :as mc]))

(defn create-sample-shops!
  []
  (shops/create! {:name "Shop 2" :latitude 90 :longitude 90})
  (shops/create! {:name "Shop 1" :latitude 17.2556458 :longitude -88.76978179999999})
  (shops/create! {:name "Fancisco" :latitude 17.2550601 :longitude -88.7642205})
  (shops/create! {:name "The Mall" :latitude 17.2488151 :longitude -88.7724495})
  (shops/create! {:name "Soundgarden" :latitude 90 :longitude 90}))

(defn create-sample-good!
  []
  (goods/create! {:barcode 12345 :name "Sample Item" :description "A Sample Item For Tests."}))

(defn reset-db!
  []
  (mc/drop db goods/goods-coll)
  (mc/drop db shops/shops-coll)
  (mc/drop db prices/prices-coll)
  (mc/drop db prices/history-coll))

