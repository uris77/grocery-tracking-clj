(ns proto.goods-search-reframe
  (:require [reagent.core :as reagent]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [re-frame.core :refer [register-handler trim-v dispatch register-sub subscribe]]
            [proto.barcode-picture :as barcode-reader]
            [proto.state :refer [get-current-location]])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction]]))

(def search-state {:barcode ""
                   :loading? false
                   :shops []
                   :item {}
                   :saving-q []
                   :current-location {}})

;;;;;;;;; Helpers ;;;;;;;;;
;;

(defn shops-url
  [barcode coords]
  (str "/api/goods/prices/nearby?barcode=" barcode "&lon=" (:lon coords) "&lat=" (:lat coords)))

(defn fetch-shops
  [barcode current-location]
  (go
    (let [shops-url (shops-url barcode current-location)
          shops-resp (<! (http/get shops-url {"accept" "application/json"}))]
      (:body shops-resp))))

(defn fetch-item
  [barcode]
  (go
    (let [good-url (str "/api/goods/barcode/" barcode)
          resp (<! (http/get good-url {"accept" "application/json"}))
          grocery (:body resp)]
      grocery)))

(defn barcode-writer
  [result]
  (if (empty? result)
    (dispatch [:no-barcode-found])
    (go (let [barcode (.-Value (nth result 0))
              grocery (<! (fetch-item barcode))]
          (if (map? grocery)
            (do
              (dispatch [:grocery-item barcode grocery])
              (let [shops (<! (fetch-shops barcode (get-current-location)))]
                (dispatch [:fetched-shops shops]))))))))

(defn scan-barcode
  [dom-event]
  (barcode-reader/picture-cb dom-event barcode-writer))


(defn save-price!
  "Saves the price for an item at a shop."
  [good shop price]
  (go
    (let [saved-price (<! (http/post "/api/shops/price" {:json-params {:shop shop :good good :price price}}))]
      (.log js/console "Saved price")
      (dispatch [:saved-price (:body saved-price)]))))


;;;;;;; Handlers ;;;;;;;;;
;;

(register-handler
 :initialise-db
 (fn [_ _]
   (assoc search-state :current-location (get-current-location))))

(register-handler
 :fetch-shops
 (fn [app-state [_ barcode]]
   app-state))


(register-handler
 :grocery-item
 (fn [app-state [_ barcode grocery-item]]
   (assoc app-state :barcode barcode :item grocery-item)))

(register-handler
 :fetched-shops
 (fn [app-state [_ shops]]
   (assoc app-state :shops shops)))

(register-handler
 :no-barcode-found
 (fn [app-state _]
   (assoc app-state :barcode :na)))

(register-handler
 :start-saving-price
 (fn [app-state [_ good shop price]]
   (save-price! good shop price)
   (update-in app-state [:saving-q] conj {:good good :shop shop :price price})))

(register-handler
 :saved-price
 (fn [app-state [_ saved-price]]
   (assoc app-state :saving-q (remove 
                               (fn [it] (= (:_id (:shop it)) (:_id (:shop saved-price)))) 
                               (:saving-q app-state)))))

(register-handler
 :change-price
 (fn [app-state [_ shop price]]
   (let [shops (:shops app-state)]
     (assoc app-state :shops 
            (mapv (fn [it] 
                   (if (= (:_id (:shop shop)) (:_id (:shop it)))
                     (assoc it :price price)
                     it)) shops)))))

;;;;;;;; Subscribers ;;;;;;;;;
;;


(register-sub
 :barcode-query
 (fn [app-state _]
   (reaction (:barcode @app-state))))

(register-sub
 :shops-query
 (fn [app-state _]
   (reaction (:shops @app-state))))

(register-sub
 :good-details-query
 (fn [app-state _]
   (reaction (:item @app-state))))

(register-sub
 :saving-q-query
 (fn [app-state _]
   (reaction (:saving-q @app-state))))

;;;;;;; Views ;;;;;;;;
;;

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

(defn good-details-panel
  [barcode]
  (let [item-details (subscribe [:good-details-query])]
    (cond
      (= @barcode :na) [:h2 "Could not read the barcode."]
      (and @item-details (not (empty? @barcode))) [:div 
                                                   [:h1 (:name @item-details)] 
                                                   [:h2 (:description @item-details)]
                                                   [:h3 "Barcode " (:barcode @item-details)]]
      (and (empty? @item-details) (not (empty? @barcode))) [:div [:h2 "No item was found with this barcode."]]
      :else [:div])))

(defn is-saving?
  [shop saving-q]
  (not (empty? (filterv 
                (fn [it] (= (:_id shop) (:_id (:shop it)))) 
                saving-q))))

(defn shops-list
  [barcode]
  (let [shops (subscribe [:shops-query])
        good (subscribe [:good-details-query])
        saving-q (subscribe [:saving-q-query])]
    (when (and (not= @barcode :na) (vector? @shops))
      [:table {:class "table table-striped table-bordered"}
       [:tbody
        (doall
         (for [shop (map (fn [it] (assoc it :key (:_id (:shop it)))) @shops)]
           [:tr {:key (:key shop)}
            [:td (:name (:shop shop))]
            [:td [:input {:type "text" 
                          :value (:price shop)
                          :on-change #(dispatch [:change-price shop (-> % .-target .-value)])} ]]
            [:td
             (cond
               (or (not (:price shop)) (empty? (:price shop))) [:button {:class "btn btn-primary btn-lg" :disabled true} "Save"]
               (is-saving? (:shop shop) @saving-q) [:button {:class "btn btn-primary btn-lg" :disabled true} "Saving"]
               :else  [:button  
                       {:class "btn btn-primary btn-lg"
                        :on-click #(dispatch [:start-saving-price @good (:shop shop) (:price shop)])} 
                       "Save"]
                )]]))]])))

(defn search-view
  []
  (let [barcode (subscribe [:barcode-query])] 
    (fn [] 
      [:div {:id "good-search-panel"}
       [search-panel]
       [:div {:id "good-details"}
        [good-details-panel barcode]]
       [:div {:id "nearby-shops"}
        [shops-list barcode]]])))

