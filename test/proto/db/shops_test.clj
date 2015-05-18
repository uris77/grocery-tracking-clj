(ns proto.db.shops-test
  (:require
   [environ.core :refer [env]]
   [clojure.test :refer :all]
   [monger.collection :as mc]
   [monger.command     :as cmd]
   [monger.result :refer [ok?]]
   [proto.db.core :refer [db conn authenticate-db! close! add-full-text!]]
   [proto.db.shops :as shops]
   [proto.db.fixtures :refer :all]
   [schema.test :as t])
  (:import com.mongodb.BasicDBObjectBuilder))

(defn enable-search
  []
  (let [bldr (doto (BasicDBObjectBuilder.)
               (.append "setParameter" 1)
               (.append "textSearchEnabled" true))
        cmd  (.get bldr)]
    (is (ok? (cmd/raw-admin-command conn cmd)))))

(use-fixtures :once
  (fn [tests]
    (authenticate-db!)
    (mc/drop db shops/shops-coll)
    (tests)))

(use-fixtures :each
  (fn [tests]
    ;;(enable-search)
    ;;(add-full-text!)
    (tests)
    ;;(mc/drop db shops/shops-coll)
    (reset-db!)))

(t/deftest create-shop-test
  (testing "Should create a shop." 
    (let [shop-params {:name "A Shop" :latitude 90.2912 :longitude 90.00012} 
          shop (shops/create! shop-params)]
      (is (some? (:_id shop)))
      (is (some? (:latitude shop)))
      (is (some? (:longitude shop))))))

(t/deftest can-not-create-shop-without-a-name-test
  (testing "Should not create a shop without a name." 
    (let [shop-params {:latitude 90 :longitude 90}
          shop (shops/create! shop-params)]
      (is (= "Name is missing!" (:name shop)))
      (is (nil? (:_id shop))))))

(t/deftest finds-shop-by-id-test
  (testing "Finds a shop with a given id." 
    (let [shop-params {:name "A Shop" :latitude 90 :longitude 90}
          shop (shops/create! shop-params)
          oid (:_id shop)
          fetched-shop (shops/get-by-id (.toString oid))]
      (is (= (:_id fetched-shop) oid))
      (is (some? (:latitude fetched-shop)))
      (is (some? (:longitude fetched-shop))))))

(t/deftest deletes-a-shop-by-id-test
  (testing "Deletes a shop with a given id." 
    (let [shop-params {:name "A Shop" :latitude 90 :longitude 90}
          shop (shops/create! shop-params)
          oid (:_id shop)]
      (shops/delete! (.toString oid))
      (is (nil? (shops/get-by-id (.toString oid)))))))

(t/deftest find-shop-by-name-test
  (testing "Find a shop by name." 
    (create-sample-shops!)
    (let [found-shop (shops/find-by-name "Shop 1")]
      (is (some? (:_id found-shop)))
      (is (some? (:latitude found-shop)))
      (is (some? (:longitude found-shop))))))

(t/deftest list-shops-test
  (testing "List shops"
    (create-sample-shops!)
    (let [shops (shops/all 0)]
      (is (= 5 (count shops))))))


;;This will only be available in mongo 3, but monger does not
;;fully support it yet.
#_(deftest search-shops-by-name-test
  (create-sample-shops!)
  (let [some-shops (shops/search-by-name "Shop")]
    (is (= 1 some-shops))
    (is (= 2 (count some-shops)))))

