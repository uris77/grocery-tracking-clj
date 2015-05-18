(ns proto.db.goods-test
  (:require
   [clojure.test :refer :all]
   [proto.db.goods :as goods]
   [proto.db.core :refer [db authenticate-db!]]
   [monger.collection :as mc]))

(use-fixtures :once
  (fn [tests]
    (authenticate-db!)
    (tests)))

(defn- reset-db!
  []
  (mc/drop db goods/goods-coll))

(deftest finds-good-by-barcode-test
  (testing "Can find a good with string or int barcode."
    (let [barcode 894334]
      (goods/create! {:barcode barcode :name "A Good With A Barcode" :description "Testing barcode search"})
      (is (= barcode (:barcode (goods/find-by-barcode barcode))))
      (is (= barcode (:barcode (goods/find-by-barcode (str barcode))))))))

