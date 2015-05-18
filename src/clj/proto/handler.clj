(ns proto.handler
  (:require [compojure.core :refer [GET POST defroutes routes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [selmer.parser :refer [render-file]]
            [prone.middleware :refer [wrap-exceptions]]
            [proto.db.goods :as goods]
            [proto.db.shops :as shops]
            [proto.db.goods-prices :as goods-prices]
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

(defn list-shops [page]
  (let [fetched-shops (shops/all (read-string page))]
    {:headers {"Content-Type" "application/json"}
     :body fetched-shops}))

(defn create-shop [req]
  (let [shop (:body req)
        created-shop (shops/create! shop)]
    {:headers {"Content-Type" "application/json"}
     :body created-shop}))

(defn find-shop-by-name
  [req]
  (let [name (get-in req [:params :name])
        shop (shops/find-by-name name)]
    {:headers {"Content-Type" "application/json"}
     :body shop}))

(defn find-goods-in-shop
  [shop-id good-name]
  (let [shop (shops/get-by-id shop-id)
        good (goods/find-by-name good-name)]
    {:headers {"Content-Type" "application/json"}
     :body (goods-prices/find-current-price-at (:_id good) (:name shop))}))

(defroutes api-routes
  (GET "/api/goods/:page" [page] (list-goods page))
  (POST "/api/goods" req (wrap-json-body create-good {:keywords? true :bigdecimals? true}))
  (GET "/api/shops/search" req find-shop-by-name)  
  (GET "/api/shops/:page" [page] (list-shops page))
  (POST "/api/shops" req (wrap-json-body create-shop {:keywords? true :bigdecimals? true}))
  (GET "/api/shops/:shop-id/price/:good-name"
       [shop-id good-name]
       (find-goods-in-shop shop-id good-name)))

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

