(ns cryogen-core.plugins
  (:require [cryogen-core.compiler :refer [compile-assets-timed]]
            [clojure.edn :as edn]
            [text-decoration.core :refer :all]))

(defn load-plugin [url]
  (let [config (edn/read-string (slurp url))]
    (println (green (str "loading module: " (:description config))))
    (require (:namespace config))))

(defn load-plugins []
  (let [plugins (.getResources (ClassLoader/getSystemClassLoader) "plugin.edn")]
    (loop []
      (load-plugin (.. plugins nextElement openStream))
      (when (.hasMoreElements plugins)
        (recur)))))
