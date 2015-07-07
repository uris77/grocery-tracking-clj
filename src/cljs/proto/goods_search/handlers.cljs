(ns proto.goods-search.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [proto.barcode-picture :as barcode-reader]
            [proto.state :refer [get-current-location]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

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

