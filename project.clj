(defproject cryogen-core "0.1.40"
            :description "Cryogen's compiler"
            :url "https://github.com/cryogen-project/cryogen-core"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.8.0"]
                           [clj-rss "0.2.3"]
                           [me.raynes/fs "1.4.6"]
                           [crouton "0.1.2"]
                           [cheshire "5.6.1"]
                           [clj-text-decoration "0.0.3"]
                           [io.aviso/pretty "0.1.26"]
                           [hiccup "1.0.5"]
                           [selmer "1.0.4"]
                           [pandect "0.5.4"]
                           [hawk "0.2.10"]
                           [clj-tagsoup "0.3.0" :exclusions [org.clojure/clojure]]])
