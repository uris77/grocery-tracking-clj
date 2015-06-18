(ns proto.db.goods
  (:import org.bson.types.ObjectId
           com.mongodb.ReadPreference)
  (:require [monger.core :as mg]
            [monger.collection :as coll]
            [monger.query :as query :refer [with-collection read-preference paginate fields limit skip snapshot]]
            [schema.core :as schema]
            [clojure.string :refer [capitalize blank?]]
            [proto.db.core :refer [db conn]]
            [proto.util :refer [validate-item]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Persistence layer for the goods.
(def goods-coll "goods")

(defn- persist-good
  [good]
  (coll/insert-and-return db goods-coll good))

(defn all 
  [page]
  (with-collection db goods-coll
    (query/find {})
    (fields [:id :barcode :name :description :categories])
    (query/sort (sorted-map :name 1))
    (skip (* page 10))
    (limit 10)))

(defn find-by-name
  "Finds a good item by its name."
  [name]
  (coll/find-one-as-map db goods-coll {:name name}))

(defn find-by-barcode
  "Finds a good item with the given barcode."
  [barcode]
  (coll/find-one-as-map db goods-coll {:barcode (str barcode)}))

(defn create! [good]
  (let [errors (validate-item good)]
    (if (empty? errors)
      (if (empty? (find-by-barcode (:barcode good)))
        (persist-good good)
        {})
      errors)))

