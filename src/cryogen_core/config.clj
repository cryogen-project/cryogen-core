(ns cryogen-core.config
  (:require [clojure.string :as string]
            [schema.core :as s]
            [cryogen-core.schemas :as schemas]
            [cryogen-core.io :as cryogen-io]))

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
    (-> config
        (update-in [:tag-root-uri] (fnil identity ""))
        (update-in [:sass-src] (fnil identity ["css"]))
        (update-in [:sass-path] (fnil identity "sass"))
        (update-in [:compass-path] (fnil identity "compass"))
        (assoc :page-root-uri (root-uri :page-root-uri config)
               :post-root-uri (root-uri :post-root-uri config)))
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
