(ns proto.barcode-picture
  (:require [reagent.core :as reagent :refer [atom]]
            [proto.state :as state]))


(def take-picture (.querySelector js/document "#take-picture"))

(def show-picture (.createElement js/document "img"))

(def canvas (.getElementById js/document "picture-region"))

(def ctx (.getContext canvas "2d"))

(defn- write-barcode!
  [barcode]
  (state/set-barcode! barcode))

(defn- image-callback [result]
  (if (> (count result) 0)
    (write-barcode! (.-Value (nth result 0)))
    (do
      (write-barcode! "")
      (prn "Decoding failed."))))

(defn- localization-callback [result]
  (.beginPath ctx)
  (set! (.-lineWidth ctx) "2")
  (set! (.-strokeStyle ctx) "red")
  (map (fn [it] (.rect ctx (.-x it) (.-y it) (.-width it) (.-height it))) result)
  (.stroke ctx))

(defn- get-image-data [results image-data]
  (set! (.-data image-data) (map (fn [data result] 
                                   (set! (.-data data) (.-data result))) 
                                 (array-seq (.-data  image-data)) (array-seq (.-data results))))
  image-data)

(defn- orientation-callback [result]
  (set! (.-width canvas) (.-width result))
  (set! (.-height canvas) (.-height result))
  (let [image (get-image-data result (.getImageData ctx 0 0 (.-width canvas) (.-height canvas)))]
    (.putImageData ctx image 0 0)))

(defn- file-reader-onload [event]
  (set! (.-onload show-picture) (fn [event] 
                                  (state/clear-barcode!)
                                  (.DecodeImage js/JOB show-picture))))

(defn- decode-image! []
  (state/clear-barcode!)
  (.DecodeImage js/JOB show-picture))

(defn- show-picture-onload [event]
  (decode-image!))

(defn start []
  (.Init js/JOB)
  (.SetImageCallback js/JOB image-callback)
  (set! (.-PostOrientation js/JOB) true)
  (set! (.-OrientationCallback js/JOB) orientation-callback)
  (.SwitchLocalizationFeedback js/JOB true)
  (.SetLocalizationCallback js/JOB localization-callback)
  (when (and take-picture show-picture)
    (set! (.-onchange take-picture) (fn [event]
                                      (write-barcode! "Decoding....")
                                      (let [files (array-seq (.-files (.-target event)))]
                                        (when (and files (> (count (seq files)) 0))
                                          (let [file (first files)]
                                            (try
                                              (set! (.-onload show-picture) show-picture-onload)
                                              (set! (.-src show-picture) ((aget js/window "URL" "createObjectURL") file))
                                              (catch js/Error e
                                                (try
                                                  (let [file-reader FileReader.]
                                                    (set! (.-onload file-reader) (fn [] 
                                                                                   (set! (.-onload show-picture) decode-image!)
                                                                                   (set! (.-src show-picture) (aget event "target" "result"))))
                                                    
                                                    (.readAsDataURL file-reader file))
                                                  (catch js/Error err
                                                    (state/clear-barcode!)) ))))))))))

