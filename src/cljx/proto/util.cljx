(ns proto.util
  #+cljs (:require-macros [schema.macros :as macros])
  (:require [schema.core :as schema]
            [clojure.string :refer [capitalize blank?]]))

(#+clj schema/def #+cljs def GroceryItem
  {:name  schema/Str
   :description schema/Str
   :barcode  #+cljs schema/Int #+clj Long
   (schema/optional-key :categories)  schema/Str
   })

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
  (let [err (schema/check GroceryItem item-map)]
    (filter-errors (gather-entity-errors (seq err)))
    (reduce into {} (pp-errors (filter-errors (gather-entity-errors (seq err)))))))

