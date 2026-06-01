(defproject cryogen-core "0.5.0"
            :description "Cryogen's compiler"
            :url "https://github.com/cryogen-project/cryogen-core"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.12.5"]
                           [camel-snake-kebab "0.4.3"]
                           [cheshire "6.2.0"]
                           [clj-rss "0.4.0"]
                           [clj-text-decoration "0.0.3"]
                           ;; used for mime type inference
                           [com.novemberain/pantomime "3.1.0"]
                           [enlive "1.1.6"]
                           [hiccup "1.0.5"]
                           [io.aviso/pretty "1.4.4"]
                           [me.raynes/fs "1.4.6"]
                           ;; used in infer-meta to extract dimensions from images
                           [net.mikera/imagez "0.12.0"]
                           ;; real name extraction
                           [org.clojars.simon_brooke/real-name "1.0.2"]
                           [pandect "1.0.2"]
                           [prismatic/schema "1.4.1"]
            [selmer "1.13.1"]]
             :managed-dependencies [[org.bouncycastle/bcpkix-jdk18on "1.81.1"]
                                    [org.bouncycastle/bcutil-jdk18on "1.81.1"]
                                    [org.bouncycastle/bcprov-jdk18on "1.81.1"]]
             :deploy-repositories [["snapshots" :clojars]
                                  ["releases" :clojars]])
