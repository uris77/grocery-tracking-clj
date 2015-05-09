(ns proto.shops
  (:require [reagent.core :as reaent]
            [cljs-http.client :as http]
            [proto.state :as state]
            [secretary.core :as secretary :include-macros true]
            [proto.util :refer [validate-shop]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn show-list
  []
  (set! (.-location js/window) (str state/base-url "/shops")))

(defn return-to-list
  []
  (state/reset-errors!)
  (state/reset-new-shop!)
  (show-list))

(defn cancel-form
  [e]
  (.preventDefault e)
  (return-to-list))

(defn fetch-shops
  [page-num]
  (go
    (let [resp (<! (http/get (str "/api/shops/" page-num) {"accept" "application/json"}))]
      (swap! state/app-state assoc-in [:shops] (:body resp)))))

(defn read-geolocation
  [position]
  (let [coords (.-coords position)
        lat (.-latitude coords)
        lon (.-longitude coords)]
      (state/set-new-shop-value! :latitude lat)
      (state/set-new-shop-value! :longitude lon)))

(defn- input-text
  "A text input"
  [id label]
  [:div {:class "form-group"}
   [:label {:class "col-xs-2 control-label"} label]
   [:div {:class "col-xs-4"}
    [:input {:class "form-control"
             :id id
             :value (id (state/get-new-shop))
             :on-change #(state/set-new-shop-value! id (-> % .-target .-value))}]]])

(defn- submit-form
  [e]
  (.preventDefault e)
  (let [errors (validate-shop (state/get-new-shop))]
    (if (empty? errors)
      (go
        (let [resp (<! (http/post "/api/shops" {:json-params (state/get-new-shop)}))]
          (prn "Got new shop " (:body resp))
          (return-to-list)))
      (state/set-errors! (vec (vals errors))))))


(defn show-errors
  [errors]
  (when (> (count errors) 0)
    [:div {:class "form-group" :style {:padding-left "15em" :width "50%"}}
     [:h3 [:span {:class "label label-danger"} "Errors while saving new shop."]]
     [:ul {:class "list-group"}
      (for [error errors]
        [:li {:class "list-group-item list-group-item-danger"} error])]]))

(defn create-shop-form
  []
  (let [new-shop (state/get-new-shop)
        errors (state/get-errors)
        navigator (aget js/window "navigator")
        geolocation (.-geolocation navigator)]
    (.getCurrentPosition geolocation read-geolocation)
    [:div
     [:form {:class "form-horizontal"}
      (show-errors errors)
      [:div {:class "form-group"}
       [:h2 {:style {:padding-left "6em"}} "Create New Shop"]]
      [:div {:class "form-group"}
       (input-text :name "Name")
       (input-text :latitude "Latitude")
       (input-text :longitude "Longitude")
       [:div {:class "form-group"}
        [:div {:class "col-xs-offset-2 col-xs-10"}
         [:button {:class "btn btn-default btn-lg col-xs-2"
                   :style {:margin-right "0.5em"}
                   :on-click cancel-form}
          "Cancel"]
         [:button {:class "btn btn-primary btn-lg col-xs-2"
                   :on-click submit-form}
          "Save"]]]]]]))


(defn- show-create-form
  []
  (set! (.-location js/window) (str state/base-url "/shops/create")))

(defn- shops-row
  "A table row for a single shop."
  [shop]
  [:tr
   [:td (:name shop)]
   [:td (:latitude shop)]
   [:td (:longitude shop)]])


(defn shops-list
  []
  [:div {:style {:padding-top "1em"}}
   [:button {:class "btn btn-lg btn-primary"
             :on-click show-create-form}
    "New Shop"]
   [:div {:style {:padding-top "1em"}}
    [:table {:class "table table-striped table-bordered"}
     [:thead
      [:tr
       [:th "Name"]
       [:th "Latitude"]
       [:th "Longitude"]]]
     [:tbody
      (for [shop (state/get-shops)]
        (shops-row shop))]]]])

