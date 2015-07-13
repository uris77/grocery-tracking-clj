(ns proto.goods-search.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [proto.goods-search.handlers :as handlers]
            [proto.goods-search.subscriptions]))



;;;;;;; Views ;;;;;;;;
;;

(defn search-panel
  "View for searching for  goods."
  []
  [:div {:class "mdl-card mdl-cell mdl-cell--12-col-desktop mdl-cell--6-col-tablet mdl-cell--4-col-phone"}
   [:canvas {:id "picture-region" :width 640 :height 480 :hidden true}]
   [:img {:id "img" :hidden true}]
   [:form 
    [:section {:class "mdl-card__supporting-text"}
     [:h5  "Read barcode from picture"]
     [:div {:class "col-xs-4"}
      [:input {:id        "take-picture"
               :class     "mdl-button mdl-js-button mdl-button--raised"
               :type      "file"
               :accept    "image/*;capture-camera"
               :on-change handlers/scan-barcode}]]]]])

(defn good-details-panel
  [barcode]
  (let [item-details (subscribe [:good-details-query])]
    (cond
      (= @barcode :na)              [:div {:class "mdl-card mdl-cell mdl-cell--12-col-desktop mdl-cell--6-col-tablet mdl-cell--4-col-phone"} 
                                     [:section {:class "mdl-card__supporting-text"}
                                      [:h3 "Could not read the barcode."]]]

      (and @item-details 
           (not (empty? @barcode))) [:div {:class "mdl-card mdl-cell mdl-cell--12-col-desktop mdl-cell--6-col-tablet mdl-cell--4-col-phone"}
                                     [:section {:class "mdl-card__supporting-text"}
                                      [:h2 (:name @item-details)] 
                                      [:h3 (:description @item-details)]
                                      [:h4 "Barcode " (:barcode @item-details)]]]

      (and (empty? @item-details) 
           (not (empty? @barcode))) [:div {:class "mdl-card mdl-cell mdl-cell--12-col-desktop mdl-cell--6-col-tablet mdl-cell--4-col-phone"} 
                                     [:section {:class "mdl-card__supporting-text"}
                                      [:h3 "No item was found."]]]
                
      :else                         [:div])))

(defn is-saving?
  [shop saving-q]
  (not (empty? (filterv 
                (fn [it] (= (:_id shop) (:_id (:shop it)))) 
                saving-q))))

(defn shops-list
  [barcode]
  (let [shops    (subscribe [:shops-query])
        good     (subscribe [:good-details-query])
        saving-q (subscribe [:saving-q-query])]
    (when (and (not= @barcode :na) (not (empty? @shops)))
      [:div {:class "mdl-card mdl-cell mdl-cell--12-col-desktop mdl-cell--6-col-tablets mdl-cell--4-col-phone mdl-grid mdl-grid--no-spacing"} 
       [:section {:class "mdl-card__supporting-text"} 
        [:h3 "Nearby Stores."]
        (doall
         (for [shop (map (fn [it] (assoc it :key (:_id (:shop it)))) @shops)]
           [:div {:class "demo-cards mdl-cell mdl-cell--4-col mdl-cell--8-col-tablet mdl-grid mdl-grid--no-spacing"
                  :key (:key shop)} 
            [:div {:class "demo-updates mdl-card mdl-shadow--2dp mdl-cell mdl-cell-4-col-tablet mdl-cell--12-col-desktop"}
             [:div {:class "mdl-card__title mdl-card--expand mdl-color--teal-300"}
              [:h2 {:class "mdl-card__title-text"} (:name (:shop shop))]]
             [:div {:class "mdl-card__actions mdl-card--border"}
              [:input {:type      "text" 
                       :class     "mdl-textfield__input"
                       :value     (:price shop)
                       :placeholder "Price"
                       :on-change #(dispatch [:change-price shop (-> % .-target .-value)])}]]
             (cond
               (or (not (:price shop)) 
                   (empty? (:price shop)))         [:button 
                                                    {:class    "mdl-button mdl-button--raised mdl-js-button mdl-js-ripple-effect" 
                                                     :disabled true} "Save"]
                   (is-saving? (:shop shop) @saving-q) [:button 
                                                        {:class    "mdl-button mdl-button--raised" 
                                                         :disabled true} "Saving"]
                   :else                               [:button  
                                                        {:class    "mdl-button mdl-button--raised mdl-button--colored"
                                                         :on-click #(dispatch [:start-saving-price @good (:shop shop) (:price shop)])} 
                                                        "Save"])]
            [:div {:class "demo-separator mdl-cell--1-col"}] ]))]])))

(defn search-view
  []
  (let [barcode (subscribe [:barcode-query])] 
    (fn []
      [:div
       [:section {:id "good-search-panel"
                  :class "section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp"}
        [search-panel]]
       [:section {:id "good-details"
                  :class "section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp"}
        [good-details-panel barcode]]
       [:section {:id "nearby-shops"
                  :class "section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp"}
        [shops-list barcode]]])))
