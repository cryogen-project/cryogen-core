(ns cryogen-core.plugins
  (:require [cryogen-core.compiler :refer [compile-assets-timed]]
            [clojure.edn :as edn]
            [clojure.string :as s]
            [text-decoration.core :refer :all]))

(defn load-plugin [url]
  (let [{:keys [description init]} (edn/read-string (slurp url))]
    (println (green (str "loading module: " description)))
    (-> init str (s/split #"/") first symbol require)
    ((resolve init))))

(defn load-plugins []
  (let [plugins (.getResources (ClassLoader/getSystemClassLoader) "plugin.edn")]
    (loop []
      (load-plugin (.. plugins nextElement openStream))
      (when (.hasMoreElements plugins)
        (recur)))))
