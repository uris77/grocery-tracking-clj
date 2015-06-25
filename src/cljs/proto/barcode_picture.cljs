(ns proto.barcode-picture
  (:require [reagent.core :as reagent :refer [atom]]
            [proto.state :as state]))


(def take-picture-el  (.querySelector js/document "#take-picture"))

(defn- show-picture [] (.getElementById js/document "img"))

(def picture-element (show-picture))

(defn- canvas-el [] (.getElementById js/document "picture-region"))

(defn- canvas-ctx [] (.getContext (canvas-el) "2d"))

(defn- write-barcode!
  [barcode]
  (state/set-barcode! barcode))

(defn- image-callback [result]
  (if (> (count result) 0)
    (write-barcode! (.-Value (nth result 0)))
    (write-barcode! "Error trying to read barcode!")))

(defn- localization-callback [result]
  (let [ctx (canvas-ctx)]
    (.beginPath ctx)
    (set! (.-lineWidth ctx) "2")
    (set! (.-strokeStyle ctx) "red")
    (map (fn [it] (.rect ctx (.-x it) (.-y it) (.-width it) (.-height it))) result)
    (.stroke ctx)))

(defn- get-image-data [results image-data]
  (set! (.-data image-data) (map (fn [data result] 
                                   (set! (.-data data) (.-data result))) 
                                 (array-seq (.-data  image-data)) (array-seq (.-data results))))
  image-data)

(defn- orientation-callback [result]
  (let [canvas (canvas-el)
        ctx (canvas-ctx)]
    (set! (.-width canvas) (.-width result))
    (set! (.-height canvas) (.-height result))
    (let [image (get-image-data result (.getImageData ctx 0 0 (.-width canvas) (.-height canvas)))]
      (.putImageData ctx image 0 0))))

(defn- file-reader-onload [event]
  (set! (.-onload (show-picture)) (fn [event] 
                                    (state/clear-barcode!)
                                    (.DecodeImage js/JOB (show-picture)))))

(defn- decode-image! []
  (let [picture-el (show-picture)]
    (state/clear-barcode!)
    (.DecodeImage js/JOB picture-el)))

(defn- show-picture-onload [event]
  (decode-image!))

(defn read-image-from-file
  [file event]
  (let [picture-element (show-picture)]
    (try
      (set! (.-onload picture-element) show-picture-onload)
      (set! (.-src picture-element) ((aget js/window "URL" "createObjectURL") file))
      (catch js/Error e
        (.log js/console "Error trying to read file " e)
        (try
          (let [file-reader FileReader.]
            (set! (.-onload file-reader) (fn [] 
                                           (set! (.-onload picture-element) decode-image!)
                                           (set! (.-src picture-element) (aget event "target" "result"))))
            (.readAsDataURL file-reader file))
          (catch js/Error err
            (state/clear-barcode!)) )))))

(defn init 
  [cb]
  (.Init js/JOB)
  (.SetImageCallback js/JOB cb)
  (set! (.-PostOrientation js/JOB) true)
  (set! (.-OrientationCallback js/JOB) orientation-callback)
  (.SwitchLocalizationFeedback js/JOB true)
  (.SetLocalizationCallback js/JOB localization-callback))

(defn picture-cb [event cb]
  (write-barcode! "Decoding....")  
  (init cb)
  (let [files (array-seq (.-files (.-target event)))]
    (when (and files (seq files))
      (read-image-from-file (first files) event))))

