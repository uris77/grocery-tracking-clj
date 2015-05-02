(ns proto.db.shops
  (:import org.bson.types.ObjectId
           com.mongodb.ReadPreference)
  (:require [proto.db.core :refer [db conn]]
            [proto.util :refer [validate-shop]]
            [monger.query :as query :refer [with-collection read-preference paginate fields limit skip snapshot]]
            [monger.collection :as coll]))

(def shops-coll "shops")

(defn create! 
  [shop]
  (let [errors (validate-shop shop)]
    (if (empty? errors)
      (coll/insert-and-return db shops-coll shop)
      errors)))

(defn all
  [page]
  (with-collection db shops-coll
    (query/find {})
    (fields [:id :name :longitude :latitude])
    (query/sort (sorted-map :name 1))
    (skip (* page 10))
    (limit 10)))

(defn get-by-id
  [id]
  (let [oid (ObjectId. id)]
    (coll/find-map-by-id db shops-coll oid)))

(defn delete!
  [id]
  (let [oid (ObjectId. id)]
    (coll/remove-by-id db shops-coll oid)))
