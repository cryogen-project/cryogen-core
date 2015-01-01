(ns cryogen-core.io
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]))

(def public "resources/public")

(defn get-resource [resource]
  (-> resource io/resource io/file))

(defn ignore [ignored-files]
  (fn [file]
    (let [name    (.getName file)
          matches (map #(re-find % name) ignored-files)]
      (not (some seq matches)))))

(defn find-assets [f ext ignored-files]
  (->> (get-resource f)
       file-seq
       (filter (ignore ignored-files))
       (filter (fn [file] (-> file .getName (.endsWith ext))))))

(defn create-folder [folder]
  (let [loc (io/file (str public folder))]
    (when-not (.exists loc)
      (.mkdirs loc))))

(defn wipe-public-folder [keep-files]
  (let [filenamefilter (reify java.io.FilenameFilter (accept [this _ filename] (not (some #{filename} keep-files))))]
    (doseq [path (.listFiles (io/file public) filenamefilter)]
      (fs/delete-dir path))))

(defn copy-images-from-markdown-folders [{:keys [blog-prefix]}]
  (doseq [asset (fs/find-files "resources/templates/md" #".+(jpg|jpeg|png|gif)")]
    (io/copy asset (io/file (str public blog-prefix "/img/" (.getName asset))))))

(defn copy-resources [{:keys [blog-prefix resources]}]
  (doseq [resource resources]
    (let [src (str "resources/templates/" resource)
          target (str public blog-prefix "/" resource)]
      (cond
        (not (.exists (io/file src)))
        (throw (IllegalArgumentException. (str "resource " src " not found")))
        (.isDirectory (io/file src))
        (fs/copy-dir src target)
        :else
        (fs/copy src target)))))
