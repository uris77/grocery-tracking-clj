(ns proto.create-goods.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :refer [create-class]]
            [proto.create-goods.handlers :refer [scan-barcode]]
            [proto.create-goods.subscriptions]
            [proto.state :as state]))



(defn submit
  [dom-event]
  (.preventDefault dom-event)
  (dispatch [:submit-new-good]))

(defn cancel
  [dom-event]
  (.preventDefault dom-event)
  (dispatch [:cancel-new-good])
  (set! (.-location js/window) (str state/base-url "/goods")))

(defn- title-view
  []
  [:div {:class "mdl-card mdl-cell mdl-cell--12-col-desktop mdl-cell--6-col-tablet mdl-cell--4-col-phone"}
   [:canvas {:id     "picture-region"
             :width  640
             :height 480
             :hidden true}]
   [:img {:id "img" :hidden true}]
   [:section {:class "mdl-card__supporting-text"}
    [:h3 "Save new grocery."]
    [:div 
     [:input {:id        "take-picture"
              :type      "file"
              :accept    "image/*;capture-camera"
              :class     "mdl-button mdl-js-button mdl-button--raised"
              :on-change scan-barcode}]]]])

(defn- register-components
  []
  (.upgradeAllRegistered js/componentHandler))

(defn- set-to-dirty
  [dom-id]
 (let [dom-element (.getElementById js/document dom-id)
       css (str (.-className dom-element) " is-dirty")]
   (set! (.-className dom-element) css)))

(defn- barcode-textfield
  []
  (let [barcode (subscribe [:new-barcode-scanned])
        dom-id "barcode-field-region"]
    (if @barcode 
      (create-class
       {:component-did-update #(set-to-dirty dom-id)
        :reagent-render      (fn []
                               [:div 
                                {:class "mdl-demo mdl-textfield mdl-js-textfield mdl-textfield--floating-label"
                                 :id dom-id}
                                [:input {:class    "mdl-textfield__input"
                                         :type     "text"
                                         :id       "barcode"
                                         :value    @barcode
                                         :on-change #()}]
                                [:label {:class "mdl-textfield__label"
                                         :for   "barcode"} "Barcode"]])})
      [:div])))

(defn- remove-class
  [existing-class class-to-remove]
  (let [classes (clojure.string/split existing-class #" ")]
    (->> classes 
         (remove #(= % class-to-remove))
         (clojure.string/join " "))))

(defn- check-error
  [dom-id field-in-model model errors]
  (let [dom-element (.getElementById js/document dom-id)
        css (str (.-className dom-element) " is-invalid")]
    (if (and (empty? (get-in model [field-in-model])) (get-in errors [field-in-model]))
      (set! (.-className dom-element) css)
      (remove-class (.-className dom-element) "is-invalid"))))

(defn- validateable-textfield
  [{:keys [label dom-id field-in-model dispatch-name]}]
  (let [good (subscribe [:new-good])
        errors (subscribe [:good-validation-errors?])]
    (create-class
     {:component-did-update #(check-error dom-id field-in-model @good @errors)
      :reagent-render (fn [{:keys [label dom-id field-in-model dispatch-name]} args]
                        [:div 
                         {:class "mdl-demo mdl-textfield mdl-js-textfield mdl-textfield--floating-label"
                          :id dom-id}
                         [:input {:class     "mdl-textfield__input"
                                  :type      "text"
                                  :id        (str field-in-model)
                                  :value     (get-in @good [field-in-model])
                                  :pattern "[A-Za-z0-9]{1,20}"
                                  :on-change #(dispatch [dispatch-name (-> % .-target .-value)] )}]
                         [:label {:class "mdl-textfield__label"
                                  :for (str field-in-model)} label]
                         [:span {:class "mdl-textfield__error"} (get-in @errors [field-in-model])]] )})))


(defn- create-form
  [good]
  (create-class 
   {:component-did-update #(register-components)
    :reagent-render 
    (fn [good]
      (when (and (some? (:barcode @good)) (not= :none (:barcode @good)))
        [:div 
         {:class "mdl-card mdl-cell mdl-cell--12-col-desktop mdl-cell--8-col-tablet mdl-cell--4-col-phone"} 
         [:section 
          {:class "mdl-card__supporting-text"}
          [:div 
           {:class "demo-cards mdl-cell mdl-cell--12-col-desktop mdl-cell--8-col-tablet mdl-cell--4-col-phone"} 
           [:form
            [barcode-textfield]
            [validateable-textfield {:label          "Grocery Name"
                                     :dom-id         "grocery-name-region"
                                     :field-in-model :name
                                     :dispatch-name  :change-name}]
            [validateable-textfield {:label          "Description"
                                     :dom-id         "grocery-description-region"
                                     :field-in-model :description
                                     :dispatch-name  :change-description}]
            [validateable-textfield {:label          "Categories"
                                     :dom-id         "grocery-categories-region"
                                     :field-in-model :categories
                                     :dispatch-name  :change-categories}]
            [:div {:class "mdl-card__actions mdl-card--border"}
             [:button {:class    "mdl-button mdl-js-button mdl-button--raised"
                       :style    {:margin-right "0.5em"}
                       :on-click cancel}
              "Cancel"]
             [:button {:class    "mdl-button mld-js-button mdl-button--raised mdl-js-ripple-effect mdl-button--colored"
                       :on-click submit}
              "Save"]]
            ]]]]))}))

(defn error-reading-barcode
  [barcode]
  (when (= :none barcode)
    [:div
     {:class "mdl-card mdl-cell mdl-cell--12-col-desktop mdl-cell--8-col-tablet mdl-cell--4-col-phone"}
     [:section
      {:class "mdl-card__supporting-text"}
      [:h3 "Could not read the barcode."]]]))


(defn create-good-view
  []
  (let [good (subscribe [:new-good])] 
    (create-class
     {:component-did-mount #(register-components)
      :reagent-render 
      (fn []
        [:div
         [:section {:class "section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp"} 
          [title-view]]
         [:section {:class "section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp"}
          [error-reading-barcode (:barcode @good)]]
         [:section {:class "section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp"}
          [create-form good]]
         ])})))

