(ns proto.db.shops-test
  (:require
   [clojure.test :refer :all]
   [monger.collection :as mc]
   [proto.db.core :refer [db conn authenticate-db! close! add-full-text!]]
   [proto.db.shops :as shops]))


(use-fixtures :once
  (fn [tests]
    (authenticate-db!)
    (tests)
    (close!)))

(use-fixtures :each
  (fn [tests]
    (mc/remove db shops/shops-coll)
    (add-full-text!)
    (tests)
    (mc/remove db shops/shops-coll)))


(deftest create-shop-test
  (let [shop-params {:name "A Shop" :latitude 90 :longitude 90} 
        shop (shops/create! shop-params)]
    (is (not= nil (:_id shop)))))

(deftest can-not-create-shop-without-a-name-test
  (let [shop-params {:latitude 90 :longitude 90}
        shop (shops/create! shop-params)]
    (is (= "Name is missing!" (:name shop)))
    (is (nil? (:_id shop)))))

(deftest finds-shop-by-id-test
  (let [shop-params {:name "A Shop" :latitude 90 :longitude 90}
        shop (shops/create! shop-params)
        oid (:_id shop)
        fetched-shop (shops/get-by-id (.toString oid))]
    (is (= (:_id fetched-shop) oid))))

(deftest deletes-a-shop-by-id-test
  (let [shop-params {:name "A Shop" :latitude 90 :longitude 90}
        shop (shops/create! shop-params)
        oid (:_id shop)]
    (shops/delete! (.toString oid))
    (is (nil? (shops/get-by-id (.toString oid))))))

