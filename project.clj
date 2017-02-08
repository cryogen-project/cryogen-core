(defproject cryogen-core "0.1.52"
            :description "Cryogen's compiler"
            :url "https://github.com/cryogen-project/cryogen-core"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.8.0"]
                           [camel-snake-kebab "0.4.0"]
                           [cheshire "5.7.0"]
                           [clj-rss "0.2.3"]
                           [clj-text-decoration "0.0.3"]
                           [enlive "1.1.6"]
                           [hawk "0.2.11"]
                           [hiccup "1.0.5"]
                           [io.aviso/pretty "0.1.33"]
                           [me.raynes/fs "1.4.6"]
                           [pandect "0.6.1"]
                           [selmer "1.10.6"]]
            :deploy-repositories [["snapshots" :clojars]
                                  ["releases" :clojars]])
