(defproject cryogen-core "0.1.25"
            :description "Cryogen's compiler"
            :url "https://github.com/cryogen-project/cryogen-core"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.7.0"]
                           [juxt/dirwatch "0.2.2"]
                           [clj-rss "0.2.0"]
                           [me.raynes/fs "1.4.6"]
                           [crouton "0.1.2"]
                           [cheshire "5.5.0"]
                           [clj-text-decoration "0.0.3"]
                           [io.aviso/pretty "0.1.18"]
                           [hiccup "1.0.5"]
                           [selmer "0.8.8"]
                           [pandect "0.5.2"]
                           [clj-tagsoup "0.3.0" :exclusions [org.clojure/clojure]]])
