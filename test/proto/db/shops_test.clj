(ns proto.db.shops-test
  (:require
   [environ.core :refer [env]]
   [clojure.test :refer :all]
   [monger.collection :as mc]
   [monger.command     :as cmd]
   [monger.result :refer [ok?]]
   [proto.db.core :refer [db conn authenticate-db! close! add-full-text!]]
   [proto.db.shops :as shops])
  (:import com.mongodb.BasicDBObjectBuilder))


(defn create-multiple-shops!
  []
  (shops/create! {:name "Shop 1" :latitude 90 :longitude 90})
  (shops/create! {:name "Shop 2" :latitude 90 :longitude 90})
  (shops/create! {:name "Placebo" :latitude 90 :longitude 90})
  (shops/create! {:name "The Clash" :latitude 90 :longitude 90})
  (shops/create! {:name "Soundgarden" :latitude 90 :longitude 90}))

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
    (tests)
    ;;(close!)
    ))

(use-fixtures :each
  (fn [tests]
    ;;(enable-search)
    ;;(add-full-text!)
    (tests)
    (mc/drop db shops/shops-coll)))

(deftest create-shop-test
  (let [shop-params {:name "A Shop" :latitude 90 :longitude 90} 
        shop (shops/create! shop-params)]
    (is (some? (:_id shop)))))

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


(deftest find-shop-by-name-test
  (create-multiple-shops!)
  (let [found-shop (shops/find-by-name "Shop 1")]
    (is (some? (:_id found-shop)))))


;;This will only be available in mongo 3, but monger does not
;;fully support it yet.
#_(deftest search-shops-by-name-test
  (create-multiple-shops!)
  (let [some-shops (shops/search-by-name "Shop")]
    (is (= 1 some-shops))
    (is (= 2 (count some-shops)))))

(deftest testing-env
  (testing "Should read test specific vars"
    (is (= "can_i_get_test" (env :db-name)))))

