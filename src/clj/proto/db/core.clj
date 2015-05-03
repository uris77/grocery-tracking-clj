(ns proto.db.core
  (:require [environ.core :refer [env]]
            [monger.core :as mg]
            [monger.collection :as coll]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Persistence global setup.

;;; Connection variables.
(def conn (mg/connect {:host (env :db-host) :port (env :db-port)}))
(def db (mg/get-db conn (env :db-name)))

(defn authenticate-db! 
  "Authenticate with mongo."
  []
  (let [username (env :db-user)
        password (.toCharArray (env :db-password))]
    (mg/authenticate db username password)))

(defn close! 
  []
  (mg/disconnect conn))

(defn add-full-text!
  []
  (coll/ensure-index db "shops" {:name "text"}))

