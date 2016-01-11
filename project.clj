(defproject cryogen-core "0.1.31"
            :description "Cryogen's compiler"
            :url "https://github.com/cryogen-project/cryogen-core"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.7.0"]
                           [clj-rss "0.2.3"]
                           [me.raynes/fs "1.4.6"]
                           [crouton "0.1.2"]
                           [cheshire "5.5.0"]
                           [clj-text-decoration "0.0.3"]
                           [io.aviso/pretty "0.1.21"]
                           [hiccup "1.0.5"]
                           [selmer "0.9.9"]
                           [pandect "0.5.4"]
                           [clj-tagsoup "0.3.0" :exclusions [org.clojure/clojure]]])
