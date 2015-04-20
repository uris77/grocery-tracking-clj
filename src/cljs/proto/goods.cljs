(ns proto.goods
  (:require [reagent.core :as reagent]
            [cljs-http.client :as http]
            [proto.state :as state])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defn fetch-goods [page]
  (go
    (let [resp (<! (http/get (str "/api/goods/" page) {"accept" "application/json"}))]
      (prn "Got back resp: " (:body resp))
      (swap! state/app-state assoc-in [:goods] (:body resp)))))

(defn- submit-form [e]
  (prn (state/get-new-good))
  (go
    (let [resp (<! (http/post "/api/goods" {:json-params (state/get-new-good)}))]
      (prn "Got response " (:body resp))))
  (.preventDefault e))

(defn new-good-text-input
  "A text input for new goods."
  [id label]
  [:div {:class "form-group"}
   [:label {:class "col-xs-2 control-label"} label]
   [:div {:class "col-xs-4"}
    [:input {:class "form-control"
             :id id
             :value (id (state/get-new-good))
             :on-change #(state/set-new-good-value! id (-> % .-target .-value))}]]])

(defn create-good-form
  []
  (let [new-good (state/get-new-good)]
    [:div
     [:form {:class "form-horizontal"}
      (new-good-text-input :barcode "Bar Code")
      (new-good-text-input :name "Name")
      (new-good-text-input :description "Description")
      (new-good-text-input :categories "Categories")
      [:div {:class "form-group"}
       [:div {:class "col-xs-offset-2 col-xs-10"}
        [:button {:class "btn btn-primary btn-lg col-xs-2"
                  :on-click submit-form} "Save"]]]]]))

(defn- good-row [good]
  [:tr
   [:td (:barcode good)]
   [:td (:name good)]
   [:td (:description good)]
   [:td (:categories good)]] )

(defn goods-list []
  [:table {:class "table table-striped table-bordered"}
   [:thead
    [:tr
     [:th "Barcode"]
     [:th "Name"]
     [:th "Description"]
     [:th "Categoriees"]]] 
   [:tbody
    (for [good (state/get-goods)]
      (good-row good))]])

