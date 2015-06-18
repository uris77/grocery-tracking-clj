(ns proto.create-prices
  (:require [reagent.core :as reagent]
            [cljs-http.client :as http]
            [proto.state :as state]
            [secretary.core :as secretary :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn find-shop-by-barcode
  [barcode]
  (go
    (let [body (<! (http/get (str "/api/goods/barcode/" barcode) {"accept" "application/json"}))
          good (:body body)]
      (state/set-good-found! good))))

(defn submit-form
  [e]
  (.preventDefault e)
  (find-shop-by-barcode (state/get-barcode-for-search)))


