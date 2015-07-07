(ns proto.goods-search.subscriptions
  (:require [re-frame.core :refer [register-sub]])
  (:require-macros [reagent.ratom :refer [reaction]]))

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

