(ns proto.state
  (:require [reagent.core :as reagent :refer [atom]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Application State
(def app-state (atom {:new-good {}
                      :goods []
                      :good-found {}
                      :new-shop {}
                      :shops []
                      :shop-name ""
                      :shop {}
                      :good-name ""
                      :barcode ""
                      :current-location {}
                      :errors []}))

(def base-url "#")

(defn get-goods []
  (get-in @app-state [:goods]))

(defn get-new-good []
  (get-in @app-state [:new-good]))

(defn add-good! [good]
  (swap! app-state conj [:goods] good))

(defn add-new-good! [good]
  (swap! app-state assoc-in [:new-good] good))

(defn set-goods! [goods]
  (swap! app-state assoc-in [:goods] goods))

(defn set-new-good-value! [id value]
  (swap! app-state assoc-in [:new-good id] value))

(defn reset-new-good! []
  (swap! app-state assoc-in [:new-good] {}))

(defn get-shops []
  (get-in @app-state [:shops]))

(defn get-new-shop []
  (get-in @app-state [:new-shop]))

(defn reset-new-shop! []
  (swap! app-state assoc-in [:new-shop] {}))

(defn app-shop! [shop]
  (swap! app-state conj [:shops] shop))

(defn add-new-shop! [shop]
  (swap! app-state assoc-in [:new-shop] shop))

(defn set-new-shop-value! [id value]
  (swap! app-state assoc-in [:new-shop id] value))

(defn get-barcode 
  "Retrieves the barcode that has been saved."
  []
  (get-in @app-state [:new-good :barcode]))

(defn set-barcode!
  "Stores a barcode that has been read from an image."
  [barcode]
  (swap! app-state assoc-in [:new-good :barcode] barcode))

(defn clear-barcode!
  "Clear the stored barcode value."
  []
  (swap! app-state assoc-in [:new-good :barcode] ""))

(defn set-errors!
  "Populates errors placeholder."
  [errors]
  (swap! app-state assoc-in [:errors] errors))

(defn get-errors
  []
  (get-in @app-state [:errors]))

(defn reset-errors!
  []
  (swap! app-state assoc-in [:errors] []))

(defn set-shop-name-for-search!
  [name]
  (swap! app-state assoc-in [:shop-name] name))

(defn get-shop-name-for-search
  []
  (get-in @app-state [:shop-name]))

(defn set-shop!
  [shop]
  (swap! app-state assoc-in [:shop] shop))

(defn get-shop
  []
  (get-in @app-state [:shop]))

(defn get-good-name-for-search
  []
  (get-in @app-state [:good-name]))

(defn set-good-name-for-search!
  [good-name]
  (swap! app-state assoc-in [:good-name] good-name))

(defn get-barcode-for-search
  []
  (get-in @app-state [:barcode]))

(defn set-barcode-for-search!
  [barcode]
  (swap! app-state assoc-in [:barcode] barcode))

(defn set-good-found!
  [good]
  (swap! app-state assoc-in [:good-found] good))

(defn get-good-found
  []
  (get-in @app-state [:good-found]))

(defn get-current-location
  []
  (get-in @app-state [:current-location]))

(defn set-current-location!
  [coords]
  (swap! app-state assoc-in [:current-location] coords))

