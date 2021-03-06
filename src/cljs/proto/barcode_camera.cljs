(ns proto.barcode-camera)

(def localized [])

(def canvas (.getElementById js/document "camera-region"))

(def ctx (.getContext canvas "2d"))

(def video (.createElement js/document "video"))

(defn- draw []
  (try 
    (.drawImage ctx video 0 0 640 480)
    (if (> (count localized) 0)
      (do 
        (.beginPath ctx)
        (set! (.-lineWidth ctx) "2")
        (set! (.-strokeStyle ctx) "red")
        (for [it localized]
          (.rect ctx (.-x it) (.-y it) (.-width it) (.-height it)))
        (.stroke ctx))
      (.setTimeout js/window draw 20))
    (catch js/Error e
      (if (= (.-name e) "NS_ERROR_NOT_AVAILABLE")
        (.setTimeout js/window draw 20)
        ))))

(def streaming false)

(def video-options (js-obj "video" true "audio" false))

(def result-container (.getElementById js/document "result-region"))

(defn user-media-fn [local-media-stream]
  (set! (.-src video) ((aget js/window "URL" "createObjectURL") local-media-stream))
  (.play video)
  (draw)
  (set! streaming true))

(defn stream-callback [result]
  (if (> (count result) 0)
    (set! (.-innerHTML result-container) (map (fn [it] (str (.Format it) " : " (.Value it))) result))))

(defn localization-callback [result]
  (set! localized result))

(defn start
  []
  (.Init js/JOB)
  (set! (.-width video) 640)
  (set! (.-height video) 480)
  (set! (.-StreamCallback js/JOB) stream-callback)
  (.SetLocalizationCallback js/JOB localization-callback)
  (.SwitchLocalizationFeedback js/JOB true)
  (let [error-fn (fn [err] (prn "The following error occurred " err))]
    (.webkitGetUserMedia js/navigator video-options user-media-fn error-fn)))
