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
        query {"good._id" good-id "shop._id" (:_id shop)}]
    (coll/find-one-as-map db prices-coll query)))

(defn find-current-price-by-barcode-at
  [barcode shop-name]
  (let [good (goods/find-by-barcode barcode)]
    (find-current-price-at (:_id good) shop-name)))

(defn history-for
  "Retrieves historical prices for a good.
  It will return a map with the following structure:
  {:_id ObjectId :good GOOD :shop SHOP :prices [{:price PRICE :date DATE}]"
  [good]
  (coll/find-one-as-map db history-coll {"good._id" (:_id good)}))

(defn history-for-good-at
  [good shop]
  (coll/find-one-as-map db history-coll {"good._id" (:_id good) "shop._id" (:_id shop)}))

(defn- format-price
  [price]
  (let [shop (get-in price [:shops :shop])
        date (get-in price [:shops :date])
        pricev (get-in price [:shops :price])
        fprice (dissoc price [:shops])]
    (assoc fprice :shops [{:shop shop :price pricev :date date}])))

(defn- format-price-for-shop
  [good shop]
  (let [persisted-price (find-current-price-at (:_id good) (:name shop))
        shops (:shops persisted-price)
        persisted-good (:good persisted-price)
        persisted-shop (take 1
                             (filter
                              (fn [it]
                                (= (:_id shop) (:_id (:shop it))) )
                              shops))
        date (:date persisted-price)
        price (:price persisted-good)]
    {:good persisted-good :price price :date date :shop persisted-shop}))

(defn- persist-update!
  [{old-price :old-price new-price :new-price}]
  (let [query {"good._id" (get-in old-price [:good :_id]) "shop._id" (get-in new-price [:shop :_id])}
        update-op {"$set" {"price" (:price new-price)}}
        shop (:shop (take 1 (:shops new-price)))]
    (coll/update db prices-coll query update-op {:multi false})
    (find-current-price-at (:_id (:good new-price)) (get-in new-price [:shop :name]))))

(defn- persist-history!
  [{old-price :old-price new-price :new-price}]
  (let [query {"good._id" (get-in old-price [:good :_id]) 
               "shop._id" (get-in old-price [:shop :_id])}
        date (:date old-price)
        price (:price old-price)
        update-op  {"$push" {"prices" {:price price :date date}}}
        persisted-history (history-for-good-at (:good new-price) (:shop new-price))]
    (if (some? persisted-history)
      (coll/update db history-coll query update-op {:multi false})
      (coll/insert-and-return db history-coll 
                              {:good (:good new-price) 
                               :shop (:shop new-price)
                               :prices [{:price price :date date}]})))) 

(defn update!
  [old-price new-price]
  (let [to-save {:old-price old-price :new-price new-price}]
    (persist-history! to-save)
    (persist-update! to-save)))

(defn insert!
  [price]
  (let [fprice (format-price price)]
    (coll/insert-and-return db prices-coll price)))

(defn save!
  "Saves a price for a good.
  It creates the price if it doesn't exist, and updates it if it already exists.
  Parameters:
      good: a db representation of the good we want to assign a price to.
      shop: a db representation of the shop we want to assign the good's price.
      price: the price we want to assign the good at the specific shop.

  Returns the saved price:
    {:_id ID :good GOOD :shop SHOP :price PRICE :date date}"
  [good shop price]
  (let [current-date (time-format/unparse date-formatter (current-date))
        persisted-price (find-current-price-at (:_id good) (:name shop))
        price {:good good :shop shop :price price :date (coerce/to-date current-date)}]
    (if (some? persisted-price)
      (update! persisted-price price)
      (insert! price) )))




