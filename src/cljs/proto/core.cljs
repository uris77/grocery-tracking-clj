(ns proto.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [cljsjs.react :as react]
            [re-frame.core :refer [dispatch-sync]]
            [proto.goods :as goods]
            [proto.shops :as shops]
            [proto.prices :as prices]
            [proto.shop-prices :as shop-prices]
            [proto.state :as state]
            [proto.goods-search.views :refer [search-view]]
            [proto.util :refer [validate-item]])
  (:import goog.History))

(defn read-geolocation
  [position]
  (let [coords (.-coords position)
        lat (.-latitude coords)
        lon (.-longitude coords)]
    (state/set-current-location! {:lon lon :lat lat})
    (state/get-current-location)))

;;(.getCurrentPosition (.-geolocation (aget js/window "navigator")) read-geolocation)


;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Welcome to proto"]
   [:div [:a {:href "#/about"} "go to about page"]]])

(defn about-page []
  [:div [:h2 "About proto"]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

(defn reset-state!
  []
  (state/clear-barcode!)
  (state/reset-new-shop!)
  (state/reset-new-good!))

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/goods/create" []
  (session/put! :current-page #'goods/create-good-form))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

(secretary/defroute "/goods" []
  (reset-state!)
  (goods/fetch-goods 0)
  (session/put! :current-page #'goods/goods-list))

(secretary/defroute "/shops" []
  (reset-state!)
  (shops/fetch-shops 0)
  (session/put! :current-page #'shops/shops-list))

(secretary/defroute "/shops/create" []
    (session/put! :current-page #'shops/create-shop-form))

(secretary/defroute "/prices" []
  (session/put! :current-page #'prices/search-form))

(secretary/defroute "/prices/:shop-name" [shop-name]
  (session/put! :current-page #'shop-prices/shop-goods-search))

(secretary/defroute "/" []
  (letfn [(init-fn [position]
            (read-geolocation position)
            (dispatch-sync [:initialise-db]))]
    (.getCurrentPosition (.-geolocation (aget js/window "navigator")) init-fn)
    (session/put! :current-page #'search-view)))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))


