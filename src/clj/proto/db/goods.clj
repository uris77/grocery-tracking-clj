(ns proto.db.goods
  (:import org.bson.types.ObjectId)
  (:require [environ.core :refer [env]]
            [monger.core :as mg]
            [monger.collection :as coll]))


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
  (when (and (some? good) (not (empty? good)))
    (coll/insert-and-return db goods-coll good)))

(defn all []
  (coll/find-maps db goods-coll))

(authenticate-db!)

