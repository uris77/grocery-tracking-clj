(ns proto.prices
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-http.client :as http]
            [proto.state :as state]
            [secretary.core :as secretary :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn find-shop-by-name
  [name]
  (go
    (let [shop (<! (http/get (str "/api/shops/search?name=" name) {"accept" "appliation/json"}))]
      (state/set-shop! (:body shop))
      (when-not (empty? (state/get-shop))
        (state/set-shop-name-for-search! "")
        (set! (.-location js/window) (str state/base-url (str "/prices/" (:name (state/get-shop)))))))))

(defn submit-shop-search-form
  [e]
  (.preventDefault e)
  (let [shop-name (.trim (state/get-shop-name-for-search))]
    (when-not (empty? shop-name)
      (find-shop-by-name shop-name))))

(defn search-form
  []
  [:div {:class "container-fluid" :style {:margin-left "10%"}}
   [:div {:class "col-xs-6" :style {:margin "0 auto"}}
    [:form {:id "custom-search-input"
            :on-submit submit-shop-search-form}
     [:div {:class "input-group col-xs-12"}
      [:input {:class "form-control input-lg"
               :placeholder "Search for a store."
               :on-change #(state/set-shop-name-for-search! (-> % .-target .-value))}]
      [:span {:class "input-group-btn"}
       [:button {:class "btn btn-info btn-lg"
                 :type "button"}
        [:i {:class "glyphicon glyphicon-search"}]]]]]
    ]

   (if-not (empty? (state/get-shop))
     [:div {:class "col-xs-6" :style {:padding-top "2em" :margin-right "2em"}}
      [:div {:class "row"}
       [:h2 "Using store " (get-in (state/get-shop) [:name])]]])])
