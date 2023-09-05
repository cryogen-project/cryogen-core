(defproject cryogen-core "0.4.3"
            :description "Cryogen's compiler"
            :url "https://github.com/cryogen-project/cryogen-core"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.10.1"]
                           [camel-snake-kebab "0.4.2"]
                           [cheshire "5.10.0"]
                           [clj-rss "0.2.6"]
                           [clj-text-decoration "0.0.3"]
                           ;; used for mime type inference
                           [com.novemberain/pantomime "2.11.0"]
                           [enlive "1.1.6"]
                           [hawk "0.2.11"]
                           [hiccup "1.0.5"]
                           [io.aviso/pretty "0.1.37"]
                           [me.raynes/fs "1.4.6"]
                           ;; used in infer-meta to extract dimensions from images
                           [net.mikera/imagez "0.12.0"]
                           ;; real name extraction
                           [org.clojars.simon_brooke/real-name "1.0.1"]
                           [pandect "0.6.1"]
                           [prismatic/schema "1.1.12"]
                           [selmer "1.12.31"]]
            :deploy-repositories [["snapshots" :clojars]
                                  ["releases" :clojars]])
