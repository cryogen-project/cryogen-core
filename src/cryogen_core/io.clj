(ns cryogen-core.io
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]))

(def public "resources/public")

(defn re-filter [bool-fn re & other-res]
  (let [res (conj other-res re)]
    (reify java.io.FilenameFilter
      (accept [this _ name]
        (bool-fn (some #(re-find % name) res))))))

(def match-re-filter (partial re-filter some?))
(def reject-re-filter (partial re-filter nil?))

(defn get-resource [resource]
  (-> resource io/resource io/file))

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
  (let [loc (io/file (str public folder))]
    (when-not (.exists loc)
      (.mkdirs loc))))

(defn wipe-public-folder [keep-files]
  (let [filenamefilter (reify java.io.FilenameFilter (accept [this _ filename] (not (some #{filename} keep-files))))]
    (doseq [path (.listFiles (io/file public) filenamefilter)]
      (fs/delete-dir path))))

(defn copy-dir [src target ignored-files]
  (fs/mkdirs target)
  (let [^java.io.FilenameFilter filename-filter (apply reject-re-filter ignored-files)
        files (.listFiles (io/file src) filename-filter)]
    (doseq [^java.io.File f files]
      (let [out (io/file target (.getName f))]
        (if (.isDirectory f)
          (copy-dir f out ignored-files)
          (io/copy f out))))))

(defn copy-resources [{:keys [blog-prefix resources ignored-files]}]
  (doseq [resource resources]
    (let [src (str "resources/templates/" resource)
          target (str public blog-prefix "/" (fs/base-name resource))]
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
  (let [theme-path (str "themes/" (:theme config))]
    (copy-resources
      (merge config
             {:resources [(str theme-path "/css")
                          (str theme-path "/js")
                          (str theme-path "/html/404.html")]}))))
