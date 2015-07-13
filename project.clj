(defproject proto "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs" "target/generated/clj" "target/generated/cljx"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring-server "0.4.0"]
                 [ring-middleware-format "0.5.0" :exclusions [org.clojure/clojure cheshire ring org.clojure/tools.reader]]
                 [cljsjs/react "0.13.1-0"]
                 [reagent "0.5.0"]
                 [reagent-forms "0.4.9"]
                 [reagent-utils "0.1.4"]
                 [re-frame "0.4.1"]
                 [org.clojure/clojurescript "0.0-3308" :scope "provided"]
                 [ring "1.3.2"]
                 [ring/ring-defaults "0.1.4"]
                 [prone "0.8.1"]
                 [compojure "1.3.3"]
                 [ring/ring-json "0.3.1"]
                 [selmer "0.8.2"]
                 [environ "1.0.0"]
                 [cheshire "5.3.1"]
                 [prismatic/schema "0.4.2"]
                 [com.novemberain/monger "2.1.0"]
                 [clj-time "0.9.0"]
                 [cljs-http "0.1.27"]
                 [secretary "1.2.3"]]

  :plugins [[lein-cljsbuild "1.0.4"]
            [lein-environ "1.0.0"]
            [lein-ring "0.9.1"]
            [lein-asset-minifier "0.2.2"]]

  :ring {:handler proto.handler/app
         :uberwar-name "proto.war"}

  :min-lein-version "2.5.0"

  :uberjar-name "proto.jar"

  :main proto.server

  :clean-targets ^{:protect false} ["resources/public/js"]

  :minify-assets
  {:assets
    {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :cljsbuild {:builds {:app {:source-paths ["src/cljs" "target/generated/cljs"]
                             :compiler {:output-to     "resources/public/js/app.js"
                                        :output-dir    "resources/public/js/out"
                                        :asset-path   "js/out"
                                        :optimizations :none
                                        :pretty-print  true}}}}

  :profiles {:dev-common {:repl-options {:init-ns          proto.repl
                                         :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl
                                                            cljx.repl-middleware/wrap-cljx]}

                   :dependencies [[ring-mock "0.1.5"]
                                  [ring/ring-devel "1.3.2"]
                                  [leiningen "2.5.1"]
                                  [weasel "0.6.0"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/tools.nrepl "0.2.10"]
                                  [pjstadig/humane-test-output "0.7.0"]
                                  [com.keminglabs/cljx "0.6.0"]]

                   :source-paths ["env/dev/clj"]
                   :plugins [[lein-figwheel "0.3.7"]
                             [lein-cljsbuild "1.0.5"]
                             [com.keminglabs/cljx "0.6.0" :exclusions [org.clojure/clojure]]]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]

                   :figwheel {:http-server-root "public"
                              :server-port 3449
                              :nrepl-port 7888
                              :css-dirs ["resources/public/css"]
                              :ring-handler proto.handler/app
                              :open-file-command "myfile_opener"}

                   :env {:dev? true
                         :squiggly {:checkers [:eastwood]
                                    :eastwood-exclude-linters [:unlimited-use]}}

                   :prep-tasks [["cljx" "once"]  "javac" "compile"]

                   :cljx {:builds [{:source-paths ["src/cljx"]
                                    :output-path "target/generated/clj"
                                    :rules :clj}
                                   {:source-paths ["src/cljx"]
                                    :output-path "target/generated/cljs"
                                    :rules :cljs}]}

                   :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]
                                              :compiler {:main "proto.dev"
                                                         :source-map true
                                                         :externs ["libs/JOB.js" "libs/DecoderWorker.js" "libs/exif.js" "libs/material.js"]}}}}}
             :dev-env-vars {}
             :dev [:dev-common :dev-env-vars]
             :test-env-vars {}
             :test-common {:env {:test? true}}
             :test [:test-common :test-env-vars]
             :uberjar {:hooks [cljx.hooks leiningen.cljsbuild minify-assets.plugin/hooks]
                       :env {:production true}
                       :aot :all
                       :omit-source true
                       :cljsbuild {:jar true
                                   :builds {:app
                                             {:source-paths ["env/prod/cljs"]
                                              :compiler
                                              {:optimizations :advanced
                                               :pretty-print false}}}}}})
