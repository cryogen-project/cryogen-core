(defproject cryogen-core "0.3.1"
            :description "Cryogen's compiler"
            :url "https://github.com/cryogen-project/cryogen-core"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.10.0"]
                           [camel-snake-kebab "0.4.1"]
                           [cheshire "5.9.0"]
                           [clj-rss "0.2.5"]
                           [clj-text-decoration "0.0.3"]
                           [enlive "1.1.6"]
                           [hawk "0.2.11"]
                           [hiccup "1.0.5"]
                           [io.aviso/pretty "0.1.37"]
                           [me.raynes/fs "1.4.6"]
                           [pandect "0.6.1"]
                           [prismatic/schema "1.1.12"]
                           [selmer "1.12.18"]]
            :deploy-repositories [["snapshots" :clojars]
                                  ["releases" :clojars]])
