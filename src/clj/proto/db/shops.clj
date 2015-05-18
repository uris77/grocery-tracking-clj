(ns proto.db.shops
  (:import org.bson.types.ObjectId
           com.mongodb.ReadPreference)
  (:require [proto.db.core :refer [db conn]]
            [proto.util :refer [validate-shop PrintableShop Shop]]
            [monger.query :as query :refer [with-collection read-preference paginate fields limit skip snapshot]]
            [monger.collection :as coll]
            [monger.search :as text-search]
            [schema.core :as s]))

(def shops-coll "shops")

(defn format-shop
  [shop]
  (-> shop
      (assoc-in [:loc] {:type "Point" :coordinates [(double (:longitude shop)) (double (:latitude shop))]})
      (dissoc :latitude :longitude)))

(s/defn pp-shop :- PrintableShop
  "Pretty print a shop by flattening its coordinates."
  [shop :- Shop]
  (let [coordinates (get-in shop [:loc :coordinates])
        longitude (first coordinates)
        latitude (last coordinates)]
    (-> shop
         (dissoc :loc)
         (assoc :longitude longitude :latitude latitude))))

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

(defn get-by-id
  [id]
  (let [oid (ObjectId. id)]
    (some->> 
          (coll/find-map-by-id db shops-coll oid)
          (pp-shop))))

(defn delete!
  [id]
  (let [oid (ObjectId. id)]
    (coll/remove-by-id db shops-coll oid)))

(defn find-by-name
  "Finds a shop by its name"
  [name]
  (some->> (coll/find-one-as-map db shops-coll {:name name})
           pp-shop))

(defn seach-by-name
  "Only available in mongo 3.0"
  [name]
  (let [search-results (text-search/search db shops-coll "Shop")]
    (text-search/results-from search-results)
    search-results))

