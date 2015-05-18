(ns proto.schemas-test
  (:require [clojure.test :refer :all]
            [schema.core :as s :include-macros true]
            [schema.test :as t]
            [proto.util :as u]))

(use-fixtures :once t/validate-schemas)

(def lon-lat 90.0013)

(defn matching? [schema value]
  (nil? (s/check schema value)))

(deftest lon-lat-schema
  (testing "The LonLat Schema"
    (is (matching? u/LonLat lon-lat))))

(deftest coordinate-scehma
  (testing "The Coordinate Schema"
    (let [coordinate [90.03434 100.33233]]
      (is (matching? u/Coordinate coordinate)))))

(deftest geolocation-schema
  (testing "The Geolocation Schema"
    (let [coordinate [90.233345 87.23435345]
          geolocation {:type "Point" :coordinates coordinate}]
      (is (matching? u/GeoLocation geolocation)))))

