(ns cryogen-core.plugins
  (:require [clojure.edn :as edn]
            [clojure.string :as string]
            [text-decoration.core :refer :all]))

(defn load-plugin [^java.net.URL url]
  (let [{:keys [description init]} (edn/read-string (slurp url))]
    (println (green (str "loading module: " description)))
    (-> init str (string/split #"/") first symbol require)
    ((resolve init))))

(defn load-plugins []
  (let [plugins (.getResources (ClassLoader/getSystemClassLoader) "plugin.edn")]
    (doseq [plugin (enumeration-seq plugins)]
      (load-plugin (. ^java.net.URL plugin openStream)))))
