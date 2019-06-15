(ns cryogen-core.io
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [me.raynes.fs :as fs]
            [text-decoration.core :refer :all]))

(def ^:dynamic public "public")

(defn set-public-path!
  [path]
  (alter-var-root #'public (constantly path)))

(defn path
  "Creates path from given parts, ignore empty elements"
  [& path-parts]
  (->> path-parts
       (remove string/blank?)
       (string/join "/")
       (#(string/replace % #"/+" "/"))))

(defn re-filter [bool-fn & res]
  (reify java.io.FilenameFilter
    (accept [this _ name]
            (bool-fn (some #(re-find % name) res)))))

(def match-re-filter (partial re-filter some?))
(def reject-re-filter (partial re-filter nil?))

(defn get-resource [resource-name]
  (let [resource (io/file resource-name)]
    (when (.exists resource)
      resource)))

(defn read-edn-resource [resource]
  (-> resource slurp read-string))

(defn ignore [ignored-files]
  (fn [^java.io.File file]
    (let [name    (.getName file)
          matches (map #(re-find % name) ignored-files)]
      (not (some seq matches)))))

(defn find-assets
  "Find all assets in the given root directory (f) and the given file
  extension (ext) ignoring any files that match the given (ignored-files).
  First make sure that the root directory exists, if yes: process as normal;
  if no, return empty vector."
  [f ^String ext ignored-files]
  (if-let [root (get-resource f)]
    (->> (get-resource f)
         file-seq
         (filter (ignore ignored-files))
         (filter (fn [^java.io.File file] (-> file .getName (.endsWith ext)))))
    []))

(defn create-folder [folder]
  (let [loc (io/file (path public folder))]
    (when-not (.exists loc)
      (.mkdirs loc))))

(defn create-file [file data]
  (spit (path public file) data))

(defn create-file-recursive [file data]
  (create-folder (.getParent (io/file file)))
  (create-file file data))

(defn wipe-public-folder [keep-files]
  (let [filenamefilter (reify java.io.FilenameFilter (accept [this _ filename] (not (some #{filename} keep-files))))]
    (doseq [path (.listFiles (io/file public) filenamefilter)]
      (fs/delete-dir path))))

(defn copy-dir [src target ignored-files]
  (fs/mkdirs target)
  (let [^java.io.FilenameFilter filename-filter (apply reject-re-filter ignored-files)
        files                                   (.listFiles (io/file src) filename-filter)]
    (doseq [^java.io.File f files]
      (let [out (io/file target (.getName f))]
        (if (.isDirectory f)
          (copy-dir f out ignored-files)
          (io/copy f out))))))

(defn copy-resources [root {:keys [blog-prefix resources ignored-files]}]
  (doseq [resource resources]
    (let [src    (path root resource)
          target (path public blog-prefix (fs/base-name resource))]
      (println "\t" (cyan src) "-->" (cyan target))
      (cond
        (not (.exists (io/file src)))
        (throw (IllegalArgumentException. (str "resource " src " not found")))
        (.isDirectory (io/file src))
        (copy-dir src target ignored-files)
        :else
        (fs/copy src target)))))

(defn copy-resources-from-theme
  "Copy resources from theme"
  [config]
  (copy-resources
   (path "themes" (:theme config))
   (merge config
          {:resources (concat
                       ["css"
                        "js"
                        "html/404.html"]
                       (:theme-resources config))})))
