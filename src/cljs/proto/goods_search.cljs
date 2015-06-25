(ns proto.goods-search
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-http.client :as http]
            [proto.state :as state]
            [proto.barcode-picture :as barcode-reader])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def page-status (atom {:current-location {:lon 0 :lat 0}
                        :good {}
                        :shops []}))

(defn good-details-panel
  []
  (let [good (get-in @page-status [:good])]
    [:div
     [:h1 (:name good)]
     [:h2 (:description good)]]))

(defn shops-list
  []
  (let [shops (get-in @page-status [:shops])]
    [:ul {:class "list-group"}
     (for [shop shops]
       [:li {:class "list-group-item"} (:name shop)])]))

(defn get-prices
  "Fetches prices in the nearby shops."
  [barcode coords]
  (go
    (let [resp (<! (http/get 
                    (str "/api/goods/prices/nearby?barcode=" barcode "&lon=" (:lon coords) "&lat=" (:lat coords) )
                    {"accept" "application/json"} ))
          good-resp (<! (http/get
                         (str "/api/goods/barcode/" barcode)
                         {"accept" "application/json"}))]
      (when-let [shops (:body resp)]
        (swap! page-status assoc-in [:shops] shops)
        (reagent/render [shops-list] (.getElementById js/document "nearby-shops")))
      (when-let [good (:body good-resp)]
        (swap! page-status assoc-in [:good] (:body good-resp))
        (reagent/render [good-details-panel] (.getElementById js/document "good-details"))))))

(defn read-geolocation
  [position]
  (let [coords (.-coords position)
        lat (.-latitude coords)
        lon (.-longitude coords)]
    (swap! page-status assoc-in [:current-location :lon] lon)
    (swap! page-status assoc-in [:current-location :lat] lat)))

(.getCurrentPosition (.-geolocation (aget js/window "navigator")) read-geolocation)

(defn submit-search
  [e]
  (.preventDefault e)
  (let [barcode (state/get-barcode-for-search)
        coords (get-in @page-status [:current-location])]
    (get-prices barcode coords)))

(defn write-barcode!
  [barcode]
  (state/set-barcode-for-search! barcode))

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
  (let [barcode (state/get-barcode-for-search)]
    [:div
     [:canvas {:id "picture-region" :width 640 :height 480 :hidden true}]
     [:img {:id "img" :hidden true}]
     [:form {:class "form-horizontal"
             :on-submit submit-search}
      [:div {:class "form-group"}
       [:label {:class "col-xs-4 control-label"} "Read barcode from picture"]
       [:div {:class "col-xs-4"}
        [:input {:id "take-picture"
                 :class "form-control btn btn-lg"
                 :type "file"
                 :accept "image/*;capture-camera"
                 :on-change scan-barcode}]]]
      [:div {:class "form-group"}
       [:label {:class "col-xs-4 control-label"} "or type Barcode"]
       [:div {:class "col-xs-4"}
        [:input {:class "form-control"
                 :value barcode
                 :on-change #(state/set-barcode-for-search! (-> % .-target .-value))}]]]
      [:div {:class "form-group"}
       [:div {:class "col-xs-offset-4 col-xs-8"}
        [:input {:class "btn btn-lg btn-primary"
                 :value "Search"
                 :type "button"
                 :on-click submit-search}]]]]]))

(defn search-view
  []
  [:div {:id "good-search-panel"}
   (search-panel)
   [:div {:id "good-details"}]
   [:div {:id "nearby-shops"}]])

