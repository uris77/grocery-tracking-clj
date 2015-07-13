(ns proto.create-goods.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [proto.barcode-picture :as barcode-reader]
            [proto.util :refer [validate-item]]
            [proto.state :as state]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def new-good-state {:good   {}
                     :errors {}
                     :saving? false})

(defn barcode-writer
  [result]
  (if (empty? result)
    (do
      (.log js/console "Could not read barcode.")
      (dispatch [:write-new-barcode :none]))
    (dispatch [:write-new-barcode (.-Value (nth result 0))])))

(defn scan-barcode
  [dom-event]
  (barcode-reader/picture-cb dom-event barcode-writer))

(defn submit
  [good]
  (go
    (let [resp (<! (http/post "/api/goods" {:json-params good}))]
      (.log js/console "Got response: " (:body resp))
      (set! (.-location js/window) (str state/base-url "/goods")))))


;;;;;;;; Handlers ;;;;;;;;;;;

(register-handler
 :write-new-barcode
 (fn [app-state [_ barcode]]
   (assoc-in app-state [:good :barcode] barcode)))

(register-handler
 :change-name
 (fn [app-state [_ name]]
   (assoc-in app-state [:good :name] name)))

(register-handler
 :change-description
 (fn [app-state [_ description]]
   (assoc-in app-state [:good :description] description)))

(register-handler
 :change-categories
 (fn [app-state [_ categories]]
   (assoc-in app-state [:good :categories] categories)))

(register-handler 
 :cancel-new-good
 (fn [app-state _]
   (dispatch [:clear-new-good-errors])
   (assoc app-state :good {})))

(register-handler
 :clear-new-good-errors
 (fn [app-state _]
   (assoc app-state :errors [])))

(register-handler
 :submit-new-good
 (fn [app-state _]
   (.log js/console "GOOD: " app-state)
   (let [good (:good app-state)
         errors (validate-item good)]
     (.log js/console "ERRORS: " errors)
     (if (empty? errors)
       (do
         (submit (:good app-state))
         (assoc app-state :saving? true))
       (do 
         (.log js/console "Validation valied " (vec (vals errors)))
         (assoc app-state :errors errors))))))

(register-handler
 :initialise-create-goods-db
 (fn [_ _]
   new-good-state))


