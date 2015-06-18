(ns proto.db.shops
  (:import org.bson.types.ObjectId
           com.mongodb.ReadPreference)
  (:require [proto.db.core :refer [db conn]]
            [proto.util :refer [validate-shop PrintableShop Shop pp-shop format-shop]]
            [monger.query :as query :refer [with-collection read-preference paginate fields limit skip snapshot]]
            [monger.collection :as coll]
            [monger.search :as text-search]
            [schema.core :as s]))

(def shops-coll "shops")
(def earth-radius 3963.2)

(defn create!
  [shop]
  (let [formatted-shop (format-shop shop)
        errors (validate-shop formatted-shop)]
    (if (empty? errors)
      (some->> (coll/insert-and-return db shops-coll formatted-shop)
               pp-shop)
      errors)))

(s/defn all :- [PrintableShop]
  [page :- long]
  (some->> (with-collection db shops-coll
            (query/find {})
            (fields [:id :name :loc])
            (query/sort (sorted-map :name 1))
            (skip (* page 10))
            (limit 10))
          seq
          (map #(pp-shop %))))

(s/defn get-by-id :- (s/maybe PrintableShop)
  [id :- String]
  (let [oid (ObjectId. id)]
    (some->> 
          (coll/find-map-by-id db shops-coll oid)
          (pp-shop))))

(s/defn delete! :- nil
  [id :- String]
  (let [oid (ObjectId. id)]
    (coll/remove-by-id db shops-coll oid)))

(defn find-by-name
  "Finds a shop by its name"
  [name]
  (some->> (coll/find-one-as-map db shops-coll {:name name})
           pp-shop))

(defn find-within
  "Find all the shops within a specific radius (in miles) from the coordinates."
  [coords, miles]
  (let [query {:loc {"$centerSphere" [coords, (/ miles earth-radius)]}}]
    (coll/find-maps db shops-coll query)))

(defn seach-by-name
  "Only available in mongo 3.0"
  [name]
  (let [search-results (text-search/search db shops-coll "Shop")]
    (text-search/results-from search-results)
    search-results))

