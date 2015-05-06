(ns proto.goods
  (:require [reagent.core :as reagent]
            [cljs-http.client :as http]
            [proto.state :as state]
            [proto.barcode-picture :as picture]
            [secretary.core :as secretary :include-macros true]
            [proto.util :refer [validate-item]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn show-list
  []
  (set! (.-location js/window) (str state/base-url "/goods")))

(defn return-to-list
  []
  (state/clear-barcode!)
  (state/reset-errors!)
  (state/reset-new-good!)
  (show-list))

(defn cancel-form
  [e]
  (.preventDefault e)
  (return-to-list))

(defn fetch-goods [page]
  (go
    (let [resp (<! (http/get (str "/api/goods/" page) {"accept" "application/json"}))]
      (prn "Got back resp: " (:body resp))
      (swap! state/app-state assoc-in [:goods] (:body resp)))))

(defn- submit-form [e]
  (prn (state/get-new-good))
  (let [errors (validate-item (state/get-new-good))]
    (if (empty? errors)
      (go
        (let [resp (<! (http/post "/api/goods" {:json-params (state/get-new-good)}))]
          (prn "Got response " (:body resp))
          (return-to-list)))
      (do
        (state/set-errors! (vec (vals errors)))
        (prn "Validation failed " (state/get-errors)))))
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


(defn show-errors
  [errors]
  (when (> (count errors) 0)
    [:div
     [:h3 [:span {:class "label label-danger"} "Errors while saving new item."]  ]
     [:ul {:class "list-group"}
      (for [error errors]
       [:li {:class "list-group-item list-group-item-danger"} error] )
      ]]))

(defn create-good-form
  []
 (let [new-good (state/get-new-good)
       barcode (:barcode new-good)
       errors (state/get-errors)]
    [:div

     [:form {:class "form-horizontal"}
      [:div {:class "form-group"}
       [:canvas {:id "picture-region" :width 640 :height 480 :hidden true}]
       [:img {:id "img" :hidden true}]
       [:input {:id "take-picture" 
                :type "file" 
                :accept "image/*;capture-camera"
                :on-change picture/picture-cb}]]
      [:div {:class "form-group" :style {:padding-left "15em" :width "50%"}}
       (show-errors errors)]
      [:div {:class "form-group"}
       [:label {:class "col-xs-2 control-label"} "Barcode"]
       [:label {:class "control-label"} barcode]]
      (new-good-text-input :name "Name")
      (new-good-text-input :description "Description")
      (new-good-text-input :categories "Categories")
      [:div {:class "form-group"}
       [:div {:class "col-xs-offset-2 col-xs-10"}
        [:button {:class "btn btn-default btn-lg col-xs-2"
                  :style {:margin-right "0.5em"}
                  :on-click cancel-form}
         "Cancel"]
        [:button {:class "btn btn-primary btn-lg col-xs-2"
                  :on-click submit-form} "Save"]]]]]) )

(defn- good-row [good]
  [:tr
   [:td (:barcode good)]
   [:td (:name good)]
   [:td (:description good)]
   [:td (:categories good)]])

(defn show-create-form
  []
  (set! (.-location js/window) (str state/base-url "/goods/create")))

(defn goods-list []
  [:div {:style {:padding-top "1em"}}
   [:button {:class "btn btn-lg btn-primary"
             :on-click show-create-form} 
    "New Grocery"]
   [:div {:style {:padding-top "1em"}}
    [:table {:class "table table-striped table-bordered"}
     [:thead
      [:tr
       [:th "Barcode"]
       [:th "Name"]
       [:th "Description"]
       [:th "Categoriees"]]] 
     [:tbody
      (for [good (state/get-goods)]
        (good-row good))]]]])

