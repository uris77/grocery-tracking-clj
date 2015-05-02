(ns proto.server
  (:require [proto.handler :refer [app]]
            [ring.adapter.jetty :refer [run-jetty]]
            [proto.db.core :as db])
  (:gen-class))

 (defn -main [& args]
   (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
     (db/authenticate-db!)
     (db/add-full-text!)
     (run-jetty app {:port port :join? false})))
