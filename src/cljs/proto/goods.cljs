(ns proto.goods
  (:require [reagent.core :as reagent]
            [goog.dom :as dom]
            [cljs-http.client :as http]
            [clojure.browser.event :as eventclj]
            [proto.state :as state]
            [proto.barcode-picture :as picture]
            [secretary.core :as secretary :include-macros true]
            [proto.util :refer [validate-item]])
  (:import goog.events.EventType)
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
  (when (seq? errors)
    [:div
     [:h3 [:span {:class "label label-danger"} "Errors while saving new item."]]
     [:ul {:class "list-group"}
      (for [error errors]
       [:li {:class "list-group-item list-group-item-danger"} error])]]))

(defn image-callback [result]
  (letfn [(write-barcode! [barcode] (state/set-barcode! barcode))]
    (if (seq result)
      (write-barcode! (.-Value (nth result 0)))
      (write-barcode! "Error trying to read barcode!"))))

(defn scan-barcode
  [dom-event]
  (picture/picture-cb  dom-event image-callback))



(defn- good-row [good]
  [:tr
   [:td {:class "mdl-data-table__cell--non-numeric"} (:barcode good)]
   [:td {:class "mdl-data-table__cell--non-numeric"} (:name good)]
   [:td {:class "mdl-data-table__cell--non-numeric"} (:description good)]
   [:td {:class "mdl-data-table__cell--non-numeric"} (:categories good)]])

(defn show-create-form
  []
  (set! (.-location js/window) (str state/base-url "/goods/create")))

(defn goods-list []
  [:div 
   [:section {:class "section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp"} 
    [:div {:class "mdl-card mld-cell mdl-cell--12-col-desktop mdl-cell--6-col-tablet mdl-cell--4-col-phone"} 
     [:section {:style {:padding-top "1em"}
            :class "mdl-card__supporting-text"}
      [:h4 "Groceries"]
      [:button {:class "mdl-button mdl-js-button mdl-button--raised mdl-button--colored"
                :on-click show-create-form} 
      "New Grocery"]
      [:table {:class "mdl-data-table mdl-js-data-table"}
       [:thead
        [:tr
         [:th {:class "mdl-data-table__cell--non-numeric"} "Barcode"]
         [:th {:class "mdl-data-table__cell--non-numeric"} "Name"]
         [:th {:class "mdl-data-table__cell--non-numeric"} "Description"]
         [:th {:class "mdl-data-table__cell--non-numeric"} "Categories"]]] 
       [:tbody
        (for [good (state/get-goods)]
          (good-row good))]]]]]])

