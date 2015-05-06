(ns proto.db.shops
  (:import org.bson.types.ObjectId
           com.mongodb.ReadPreference)
  (:require [proto.db.core :refer [db conn]]
            [proto.util :refer [validate-shop]]
            [monger.query :as query :refer [with-collection read-preference paginate fields limit skip snapshot]]
            [monger.collection :as coll]
            [monger.search :as text-search]))

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
    (fields [:id :name :longitude :latitude :categories])
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

(defn find-by-name
  "Finds a shop by its name"
  [name]
  (coll/find-one-as-map db shops-coll {:name name}))

(defn seach-by-name
  "Only available in mongo 3.0"
  [name]
  (let [search-results (text-search/search db shops-coll "Shop")]
    (text-search/results-from search-results)
    search-results))

