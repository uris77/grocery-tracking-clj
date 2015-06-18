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
(def history-coll "prices_history")

(def date-formatter (time-format/formatter "yyyy-MM-dd hh:mm:ss"))

(defn current-date
  []
  (time/from-time-zone (time/now) (time/time-zone-for-offset +6)))

(defn find-by-good
  [good]
  (let [good-id (:_id good)]
    (coll/find-one-as-map db prices-coll {"good._id" good-id})))

(defn find-history-by-good
  [good]
  (let [good-id (:_id good)]
    (coll/find-one-as-map db history-coll {"good._id" good-id})))

(defn find-current-price-at
  "Returns the current price for the specified good at a shop."
  [good-id shop-name]
  (let [shop (shops/find-by-name shop-name)
        query {"good._id" good-id}
        price-for-good (coll/find-one-as-map db prices-coll query)]
    (first (filterv (fn [it] (= shop-name (get-in it [:shop :name]))) (:shops price-for-good)))))

(defn find-current-price-by-barcode-at
  [barcode shop-name]
  (let [good (goods/find-by-barcode barcode)]
    (find-current-price-at (:_id good) shop-name)))

(defn history-for
  "Retrieves historical prices for a good.
  It will return a map with the following structure:
  {:_id ObjectId :good GOOD :history [{:shop SHOP :prices [:date DATE :price PRICE}]"
  [good]
  (coll/find-one-as-map db history-coll {"good._id" (:_id good)}))

(defn history-for-good-at
  [good shop]
  (let [history (history-for good)]
    (first (filterv (fn [it] (= (:_id shop) (get-in it [:shop :_id]))) (:history history)))))

(defn- format-price
  [price]
  (let [shop (get-in price [:shops :shop])
        date (get-in price [:shops :date])
        pricev (get-in price [:shops :price])
        fprice (dissoc price [:shops])]
    (assoc fprice :shops [{:shop shop :price pricev :date date}])))

(defn- persist-update!
  [{old-price :old-price new-price :new-price}]
  (let [query {"good._id" (get-in old-price [:good :_id]) "shops.shop._id" (get-in new-price [:shops :shop :_id])}
        update-op {"$set" {"shops.$.price" (get-in new-price [:shops :price])}}]
    (coll/update db prices-coll query update-op {:multi false})
    (find-by-good (:good old-price))))

(defn- persist-history!
  [{old-price :old-price new-price :new-price}]
  (let [query {"good._id" (get-in old-price [:good :_id]) "history.shop.id" (get-in old-price [:shops :shop :_id])}
        date (get-in new-price [:shops :date])
        price (:price (first (filterv (fn [it] (= (get-in it [:shop :_id]) (get-in new-price [:shops :shop :_id]))) (:shops old-price))) )
        update-op  {$push {"history.$.prices" {:date date :price price}}}
        persisted-history (find-history-by-good (get-in old-price [:good]))]
    (if (some? persisted-history)
      (coll/update db history-coll query update-op {:multi false})
      (coll/insert-and-return db history-coll 
                              {:good (:good old-price) 
                               :history 
                               [{:shop (get-in new-price [:shops :shop])
                                 :prices [{:date date :price price}]}]})))) 

(defn update!
  [old-price new-price]
  (let [to-save {:old-price old-price :new-price new-price}]
    (persist-history! to-save)
    (persist-update! to-save)))

(defn insert!
  [price]
  (let [fprice (format-price price)]
    (coll/insert-and-return db prices-coll fprice)))

(defn save!
  "Saves a price for a good.
  It creates the price if it doesn't exist, and updates it if it already exists.
  Parameters:
      good: a db representation of the good we want to assign a price to.
      shop: a db representation of the shop we want to assign the good's price.
      price: the price we want to assign the good at the specific shop.

  Returns the saved price:
    {:_id ID :good GOOD :shops [{:shop SHOP :price PRICE :date date]"
  [good shop price]
  (let [current-date (time-format/unparse date-formatter (current-date))
        persisted-price (find-by-good good)
        price {:good good :shops {:shop shop :price price :date (coerce/to-date current-date)}}]
    (if (some? persisted-price)
      (update! persisted-price price)
      (insert! price) )))

