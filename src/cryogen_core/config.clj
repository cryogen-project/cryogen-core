(ns cryogen-core.config
  (:require [clojure.string :as string]
            [clojure.set :as set]
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
                     (update-in [:public-dest] (fnil identity "public"))
                     (update-in [:recent-posts] (fnil identity 3))
                     (update-in [:archive-group-format] (fnil identity "yyyy MMMM"))
                     (update-in [:sass-src] (fnil identity ["css"]))
                     (update-in [:sass-path] (fnil identity "sass"))
                     (update-in [:posts-per-page] (fnil identity 5))
                     (update-in [:blocks-per-preview] (fnil identity 2))
                     (update-in [:copy-html] (fnil identity ["404"]))
                     (assoc :page-root-uri (root-uri :page-root-uri config)
                            :post-root-uri (root-uri :post-root-uri config)))
          check-overlap (fn [dirs]
                          (some #(subpath? % (:public-dest config)) dirs))
          check-html (fn [& htmls]
                       (apply set/intersection (map set htmls)))]
      (cond
        (or (= (string/trim (:public-dest config)) "")
            (string/starts-with? (:public-dest config) ".")
            (check-overlap ["content" "themes" "src" "target"])) (throw (new Exception "Dangerous :public-dest value. The folder will be deleted each time the content is rendered. Specify a sub-folder that doesn't overlap with the default folders or your resource folders."))
        (seq (check-html (:copy-html config) (:compile-html config))) (throw (new Exception ":copy-html and :compile-html contain overlapping values. We can't compile an html file and copy it as-is at the same time. Please modify those settings to contain distinct values"))
        :else config))
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
  (let [config (-> "content/config.edn"
                   cryogen-io/get-resource
                   cryogen-io/read-edn-resource)
        theme-config-resource (-> config
                                  :theme
                                  (#(cryogen-io/path "themes" % "config.edn"))
                                  cryogen-io/get-resource)
        theme-config (if theme-config-resource
                       (cryogen-io/read-edn-resource theme-config-resource))]
    (assoc config
           :theme-resources
           (or (:resources theme-config) [])
           :theme-sass-src
           (or (:sass-src theme-config) []))))

(defn resolve-config
  "Loads the config file, merging in the overrides and, and filling in missing defaults"
  ([]
   (resolve-config {}))
  ([overrides]
   (process-config (deep-merge true (read-config) overrides))))
