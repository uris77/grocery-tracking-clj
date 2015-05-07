(ns proto.state
  (:require [reagent.core :as reagent :refer [atom]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Application State
(def app-state (atom {:new-good {}
                      :goods []
                      :new-shop {}
                      :shops []
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

(defn set-new-good-value! [id value]
  (swap! app-state assoc-in [:new-good id] value))

(defn reset-new-good! []
  (swap! app-state assoc-in [:new-good] {}))

(defn get-shops []
  (get-in @app-state [:shops]))

(defn get-new-shop []
  (get-in @app-state [:new-shop]))

(defn reset-new-shop! []
  (swap! app-state assoc-in [:new-good] {}))

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

(defn reset-new-shop!
  []
  (swap! app-state assoc-in [:new-shop] {}))

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


