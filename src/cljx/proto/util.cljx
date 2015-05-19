(ns proto.util
  #+cljs (:require-macros [schema.macros :as macros])
  (:require [schema.core :as s]
            [clojure.string :refer [capitalize blank?]])
  (:import org.bson.types.ObjectId))

(s/defschema LonLat (s/named #+cljs s/Int #+clj Double "LonLat"))

(s/defschema Coordinate 
  (s/named 
   [(s/one LonLat "lon") (s/one LonLat "lat")] "Coordinate"))

(s/defschema GeoLocation
  (s/named
   {:type s/Str
    :coordinates Coordinate}
   "GeoLocation"))

(s/defschema GroceryItem
  {:name s/Str
   :description s/Str
   :barcode #+cljs s/Int #+clj Long
   (s/optional-key :categories) s/Str})

#_(#+clj s/def #+cljs def GroceryItem
  {:name  s/Str
   :description s/Str
   :barcode  #+cljs s/Int #+clj Long
   (s/optional-key :categories)  s/Str
   })

(s/defschema Shop
  {(s/optional-key :_id) #+cljs s/Int #+clj ObjectId
   :name s/Str
   :loc GeoLocation})

(s/defschema PrintableShop
  {(s/optional-key :_id) #+cljs s/Int #+clj ObjectId
   :name s/Str
   :longitude LonLat
   :latitude LonLat})

(s/defschema ShopValidationErrors
  {(s/optional-key :name) s/Str
   (s/optional-key :longitude) s/Str
   (s/optional-key :latitude) s/Str})

(defn- item-error-parser 
  "Makes an error message more readable."
  [error-msg]
  (cond
   (= (str error-msg) "missing-required-key") "is missing!"
   (= (str error-msg) "disallowed-key") "is an invalid field!"
   :else ""))

(defn- pp-errors 
  "Prepends the error message with the corresponding field."
  [errors]
  (for [error errors]
    (do (let [k (first (keys error))
              v (first (vals error))]
          {k (str (capitalize (name k)) " " v)}))))

(defn- gather-entity-errors 
  "Maps over a list of errors converting the error messages into more readable strings."
  [errors]
  (vec (map (fn [it]
              {(first it) (item-error-parser (last it))}) errors)))

(defn- filter-errors 
  "Removes items whose error message is empty."
  [errors]
  (remove (fn [x]
            (blank? (first (vals  x))))
          errors))

(defn validate-item 
  "Validates a grocery item."
  [item-map]
  (let [err (s/check GroceryItem item-map)]
    (filter-errors (gather-entity-errors (seq err)))
    (reduce into {} (pp-errors (filter-errors (gather-entity-errors (seq err)))))))

(defn validate-shop
  "Validates a shop"
  [shop-map]
  (let [err (s/check Shop shop-map)]
    (filter-errors (gather-entity-errors (seq err)))
    (reduce into {} (pp-errors (filter-errors (gather-entity-errors (seq err)))))))

