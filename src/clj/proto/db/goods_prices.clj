(ns proto.db.goods-prices
  (:require
   [monger.core :as mg]
   [monger.collection :as coll]
   [monger.query :as query :refer [with-collection read-preference paginate fields limit skip snapshot]]
   [clj-time.core :as time]
   [clj-time.format :as time-format]
   [proto.db.core :refer [db conn]]
   [proto.util :refer [validate-item]]))

(def prices-coll "items_current_prices")

(def date-formatter (time-format/formatter "yyyy-MM-dd hh:mm:ss"))

(defn current-date
  []
  (time/from-time-zone (time/now) (time/time-zone-for-offset +6)))

(defn save!
  "Saves a price for a good."
  [good shop price]
  (let [current-date (time-format/unparse date-formatter (current-date))
        price {:good good :shop shop :price price :date current-date}]
    (coll/insert-and-return db prices-coll price)))


