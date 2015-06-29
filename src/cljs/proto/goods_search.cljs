(ns proto.goods-search
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-http.client :as http]
            [cljs.core.async :as async
             :refer [>! <! put! chan]]
            [proto.barcode-picture :as barcode-reader]
            [proto.state :as state])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defn by-id
  [id]
  (.getElementById js/document id))

(defn good-details-panel
  [good barcode]
  (if (empty? good)
    [:div
     [:h1 "No Item found with this barcode. (" barcode ")"]]
    [:div
     [:h1 (:name good)]
     [:h2 (:description good)]
     [:h3 "Barcode: " barcode]]))

(defn show-good-details
  [goods-chan barcode]
  (go
    (when-let [good (<! goods-chan)]
      (reagent/render [good-details-panel good barcode] (by-id "good-details")))))

(defn shops-list
  [shops]
  (when (vector? shops)
    [:ul {:class "list-group"}
     (for [shop shops]
       [:li {:class "list-group-item"} (:name shop)])]))

(defn show-shops
  [barcode coords]
  (go 
    (let [shops-chan (chan 1)
          prices-url (str "/api/goods/prices/nearby?barcode=" barcode "&lon=" (:lon coords) "&lat=" (:lat coords))
          shops-resp (<! (http/get prices-url {"accept" "application/json"}))
          shops (:body shops-resp)]
      (reagent/render [shops-list shops] (by-id "nearby-shops")))))

(defn get-prices
  "Fetches prices in the nearby shops."
  [barcodec coords]
  (go
    (let [barcode (<! barcodec)
          good-url (str "/api/goods/barcode/" barcode)
          good-resp (<! (http/get good-url {"accept" "application/json"}))
          good (:body good-resp)
          goods-chan (chan 1)] 
      (show-good-details goods-chan barcode)
      (if (map? good)
        (do
          (put! goods-chan good)
          (show-shops barcode coords))
        (do 
          (put! goods-chan {})
          (reagent/render [:div] (by-id "nearby-shops")))))))

(defn write-barcode!
  [barcode]
  (let [barcode-chan (chan 1)]
    (put! barcode-chan barcode)
    (get-prices barcode-chan (state/get-current-location))))

(defn barcode-writer
  "Writes the scanned barcode into an atom."
  [result]
  (if (seq result)
    (write-barcode! (.-Value (nth result 0)))
    (write-barcode! "Error trying to read barcode!")))

(defn scan-barcode
  [dom-event]
  (barcode-reader/picture-cb dom-event barcode-writer))

(defn search-panel
  "View for searching for  goods."
  []
  [:div
   [:canvas {:id "picture-region" :width 640 :height 480 :hidden true}]
   [:img {:id "img" :hidden true}]
   [:form {:class "form-horizontal"}
    [:div {:class "form-group"}
     [:label {:class "col-xs-4 control-label"} "Read barcode from picture"]
     [:div {:class "col-xs-4"}
      [:input {:id "take-picture"
               :class "form-control btn btn-lg"
               :type "file"
               :accept "image/*;capture-camera"
               :on-change scan-barcode}]]]]])

(defn search-view
  []
  [:div {:id "good-search-panel"}
   (search-panel)
   [:div {:id "good-details"}]
   [:div {:id "nearby-shops"}]])

