(ns cryogen-core.config
  (:require [clojure.string :as string]
            [schema.core :as s]
            [cryogen-core.schemas :as schemas]
            [cryogen-core.io :as cryogen-io]))

(defn subpath?
  "Checks if either path is a subpath of the other"
  [p1 p2]
  (let [parts #(string/split % #"/")]
    (every? #{true} (map #(= %1 %2) (parts p1) (parts p2)))))

(defn root-uri
  "Creates the uri for posts and pages. Returns root-path by default"
  [k config]
  (if-let [uri (k config)]
    uri
    (config (-> k (name) (string/replace #"-uri$" "") (keyword)))))

(defn process-config
  "Reads the config file"
  [config]
  (try
    (s/validate schemas/Config config)
    (let [config (-> config
                     (update-in [:tag-root-uri] (fnil identity ""))
                     (update-in [:sass-src] (fnil identity ["css"]))
                     (update-in [:sass-path] (fnil identity "sass"))
                     (update-in [:compass-path] (fnil identity "compass"))
                     (update-in [:public-dest] (fnil identity "public"))
                     (assoc :page-root-uri (root-uri :page-root-uri config)
                            :post-root-uri (root-uri :post-root-uri config)))
          check-overlap (fn [dirs]
                          (some #(subpath? % (:public-dest config)) dirs))]
      
      (if (or (= (string/trim (:public-dest config)) "")
              (string/starts-with? (:public-dest config) ".")
              (check-overlap ["content" "themes" "src" "target"])
              (check-overlap (:resources config)))
        (throw (new Exception "Dangerous :public-dest value. The folder will be deleted each time the content is rendered. Specify a sub-folder that doesn't overlap with the default folders or your resource folders."))
        config))
    (catch Exception e (throw e))))

(defn deep-merge
  "Recursively merges maps. When override is true, for scalars and vectors,
  the last value wins. When override is false, vectors are merged, but for
  scalars, the last value still wins."
  [override & vs]
  (cond
    (= (count vs) 1) vs
    (every? map? vs) (apply merge-with (partial deep-merge override) vs)
    (and (not override) (every? sequential? vs)) (apply into vs)
    :else (last vs)))

(defn read-config []
  (let [config (-> "config.edn"
                   cryogen-io/get-resource
                   cryogen-io/read-edn-resource)
        theme-config-resource (-> config
                                  :theme
                                  (#(cryogen-io/path "themes" % "config.edn"))
                                  cryogen-io/get-resource)]
    (if (and (:theme config) theme-config-resource)
      (deep-merge false (cryogen-io/read-edn-resource theme-config-resource) config)
      config)))

(defn resolve-config
  "Loads the config file, merging in the overrides and, and filling in missing defaults"
  [overrides]
  (process-config (deep-merge true (read-config) overrides)))
