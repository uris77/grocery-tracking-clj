(ns proto.create-goods.subscriptions
  (:require [re-frame.core :refer [register-sub]])
  (:require-macros [reagent.ratom :refer [reaction]]))

(register-sub
 :new-good
 (fn [app-state _]
   (reaction (:good @app-state))))

(register-sub
 :good-validation-errors?
 (fn [app-state _]
   (reaction (:errors @app-state))))

(register-sub
 :new-barcode-scanned
 (fn [app-state _]
   (reaction (get-in @app-state [:good :barcode]))))

