(ns proto.handler
  (:require [compojure.core :refer [GET POST defroutes routes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [selmer.parser :refer [render-file]]
            [prone.middleware :refer [wrap-exceptions]]
            [proto.db.goods :as goods]
            [cheshire.core :refer :all]
            [environ.core :refer [env]]))

(defn list-goods [page]
  (let [fetched-goods (goods/all (read-string page))]
    {:headers {"Content-Type" "application/json"}
     :body fetched-goods}))

(defn create-good [req]
  (let [good (:body req)
        created-good (goods/create! good)]
    {:headers {"Content-Type" "application/json"}
     :body created-good}))

(defroutes api-routes
  (GET "/api/goods/:page" [page] (list-goods page))
  (POST "/api/goods" req (wrap-json-body create-good {:keywords? true :bigdecimals? true})))

(defroutes app-routes
  (GET "/" [] (render-file "templates/main.html" {:dev (env :dev?)}))
  (resources "/")
  (not-found "Not Found"))

(defn api-app []
  (cheshire.generate/add-encoder org.bson.types.ObjectId cheshire.generate/encode-str)
  (wrap-restful-format (wrap-defaults api-routes api-defaults)))

(def app
  (let [handler (routes (api-app) (wrap-defaults app-routes site-defaults)) ]
    (if (env :dev?) (wrap-exceptions handler) handler)))

