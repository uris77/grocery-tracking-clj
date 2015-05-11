(ns proto.db.goods-prices
  (:require
   [monger.core :as mg]
   [monger.collection :as coll]
   [monger.query :as query :refer [with-collection read-preference paginate fields limit skip snapshot]]
   [monger.operators :refer :all]
   [clj-time.core :as time]
   [clj-time.format :as time-format]
   [clj-time.coerce :as coerce]
   [proto.db.core :refer [db conn]]
   [proto.db.goods :as goods]
   [proto.db.shops :as shops]
   [proto.util :refer [validate-item]]))

(def prices-coll "items_current_prices")

(def date-formatter (time-format/formatter "yyyy-MM-dd hh:mm:ss"))

(defn current-date
  []
  (time/from-time-zone (time/now) (time/time-zone-for-offset +6)))

(defn find-by-good
  [good]
  (let [good-id (:_id good)]
    (coll/find-one-as-map db prices-coll {"good._id" good-id})))

(defn- format-price
  [price]
  (let [shop (get-in price [:shops :shop])
        date (get-in price [:shops :date])
        pricev (get-in price [:shops :price])
        fprice (dissoc price [:shops])]
    (assoc fprice :shops [{:shop shop :price pricev :date date}])))

(defn update!
  [old-price new-price]
  (let [query {:_id (:_id old-price) "shops.shop.name" (get-in new-price [:shops :shop :name])}
        update-op {$set {"shops.$.price" (get-in new-price [:shops :price])}}]
    (coll/update db prices-coll query update-op {:multi false} )))

(defn insert!
  [price]
  (let [fprice (format-price price)]
    (coll/insert-and-return db prices-coll fprice)))

(defn save!
  "Saves a price for a good."
  [good shop price]
  (let [current-date (time-format/unparse date-formatter (current-date))
        persisted-price (find-by-good good)
        price {:good good :shops {:shop shop :price price :date (coerce/to-date current-date)}}]
    (if persisted-price
      (update! persisted-price price)
      (insert! price) )))

(defn find-current-price-at
  [good shop-name]
  (let [shop (shops/find-by-name shop-name)
        query {"good._id" (:_id good)}
        price-for-good (coll/find-one-as-map db prices-coll query)]
    (first (filterv (fn [it] (= shop-name (get-in it [:shop :name]))) (:shops price-for-good)))))


