(ns proto.shop-prices
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-http.client :as http]
            [proto.state :as state]
            [secretary.core :as secretary :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn find-good-by-barcode
  [barcode]
  (go
    (let [body (<! (http/get (str "/api/goods/barcode/" barcode) {"accept" "application/json"}))
          good (:body body)]
      (state/set-good-found! good)
      (prn "Good Details: " good))))

(defn find-good-in-shop
  [shop good-name]
  (find-good-by-barcode good-name)
  (go
    (let [goods (<! (http/get (str "/api/shops/" (:_id shop) "/price/" good-name) {"accept" "application/json"}))]
      (state/set-goods! (:body goods))
      (prn "FOUND GOODS: " (state/get-goods)))))

(defn submit-goods-search-form
  [e]
  (.preventDefault e)
  (find-good-in-shop (state/get-shop) (state/get-good-name-for-search)))

(defn good-description
  []
  (let [good-found (state/get-good-found)]
    [:div {:style {:margin-top "8%"}}
     [:h3 (:name good-found)]
     [:h4 (:description good-found)]
     (:categories good-found)]))

(defn shop-goods-search
  []
  (let [shop-name (:name (state/get-shop))]
    [:div {:class "container-fluid" :style {:margin-left "10%"}}
     [:div {:class "col-xs-6" :style {:margin "0 auto"}}
      [:form {:id "custom-search-input"
              :on-submit submit-goods-search-form}
       [:div {:class "input-group col-xs-12"}
        [:input {:class "form-control input-lg"
                 :placeholder (str  "Search for goods and items in " shop-name)
                 :on-change #(state/set-good-name-for-search! (-> % .-target .-value))}]
        [:span {:class "input-group-btn"}
         [:button {:class "btn btn-info btn-lg"
                   :type "button"}
          [:i {:class "glyphicon glyphicon-search"}]]]]]]
     (good-description)]))

