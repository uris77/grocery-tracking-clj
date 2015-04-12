(ns proto.state
  (:require [reagent.core :as reagent :refer [atom]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Application State
(def app-state (atom {:new-good {}
                      :goods []}))

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

