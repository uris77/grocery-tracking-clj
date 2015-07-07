(ns proto.goods-search.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [proto.goods-search.handlers :as handlers]
            [proto.goods-search.subscriptions]))

(def search-state {:barcode ""
                   :loading? false
                   :shops []
                   :item {}
                   :saving-q []
                   :current-location {}})

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
               :on-change handlers/scan-barcode}]]]]])

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
               (or (not (:price shop)) 
                   (empty? (:price shop))) [:button 
                                            {:class "btn btn-primary btn-lg" 
                                             :disabled true} "Save"]
               (is-saving? (:shop shop) @saving-q) [:button 
                                                    {:class "btn btn-primary btn-lg" 
                                                     :disabled true} "Saving"]
               :else  [:button  
                       {:class "btn btn-primary btn-lg"
                        :on-click #(dispatch [:start-saving-price @good (:shop shop) (:price shop)])} 
                       "Save"])]]))]])))

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

