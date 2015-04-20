(ns proto.db.goods
  (:import org.bson.types.ObjectId
           com.mongodb.ReadPreference)
  (:require [environ.core :refer [env]]
            [monger.core :as mg]
            [monger.collection :as coll]
            [monger.query :as query :refer [with-collection read-preference paginate fields limit skip snapshot]]
            [schema.core :as schema]
            [clojure.string :refer [capitalize blank?]]
            [proto.util :refer [validate-item]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Persistence layer for the goods.

;;; Connection variables.
(def conn (mg/connect {:host (env :db-host) :port (env :db-port)}))
(def db (mg/get-db conn (env :db-name)))
(def goods-coll "goods")

;;; Authenticate with mongo
(defn authenticate-db! []
  (let [username (env :db-user)
        password (.toCharArray (env :db-password))]
    (mg/authenticate db username password)))

(defn create! [good]
  (let [errors (validate-item good)]
    (if (empty? errors)
      (coll/insert-and-return db goods-coll good)
      errors)))

(defn all 
  [page]
  (with-collection db goods-coll
    (query/find {})
    (fields [:id :barcode :name :description :cateegories])
    (query/sort (sorted-map :name 1))
    (skip (* page 10))
    (limit 10)))

(authenticate-db!)

