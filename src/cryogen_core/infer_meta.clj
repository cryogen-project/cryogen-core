(ns cryogen-core.infer-meta
  (:require [clojure.java.io :refer [reader]]
            [clojure.pprint :refer [pprint]]
            [clojure.string :refer [capitalize join lower-case replace]]
            [cryogen-core.markup :refer [exts render-fn]]
            [cryogen-core.util :refer [parse-post-date trimmed-html-snippet]])
  (:import [java.util Date Locale]
           [java.nio.file Files FileSystems LinkOption]
           [java.nio.file.attribute FileOwnerAttributeView]))

;; see https://gist.github.com/saidone75/0844f40d5f2d8b129cb7302b7cf40541
(defn file-attribute
  "Return the value of the specified `attribute` of the file at `file-path`
   in the current default file system. The argument `attribute` may be passed
   as a keyword or a string, but must be an attribute name understood be 
   `java.nio.file.Files`."
  [file-path attribute]
  (Files/getAttribute
   (.getPath
    (FileSystems/getDefault)
    (str file-path)
    (into-array java.lang.String []))
   (name attribute)
   (into-array LinkOption [])))

(defn- maybe-extract-date-from-filename [page config]
  (try (parse-post-date (.getName page) (:post-date-format config))
       (catch Exception _ nil)))

;; copied from compile; if I'm using this solution I should be moving this function
;; to some common utility namespacw
(defn- re-pattern-from-exts
  "Creates a properly quoted regex pattern for the given file extensions"
  [exts]
  (re-pattern (str "(" (join "|" (map #(replace % "." "\\.") exts)) ")$")))


(defn infer-file-name
  "The general pattern for Cryogen post names is date in `yyyy-mm-dd` format, 
   followed by hyphen, followed by the title of the post lower-cased and with
   hyphens substituted for spaces."
  [^java.io.File page meta config]
  (if (:title meta)
    (str (:date meta) "-" (replace (lower-case (:title meta)) #" +" "-") ".html")
    (let [re-root     (re-pattern (str "^.*?(" (:page-root config) "|" (:post-root config) ")/"))
          page-fwd    (replace (str page) "\\" "/")  ;; make it work on Windows
          page-name   (if (:collapse-subdirs? config)
                        (.getName page)
                        (replace page-fwd re-root ""))]
      (replace page-name (re-pattern-from-exts
                          [".md"]  ;;(exts markup)
                          )".html"))))

(defn infer-title
  "Infer the title of this page, ideally by extracting the first `H1` element from this
   `dom` (Document Object Model) of its content, given this `config`."
  ;; dom turns out to be a list of maps; I'm not yet certain what happens when elements
  ;; have child elements but for the time being it doesn't matter.
  [^java.io.File page config dom]
  (pprint dom)
  (let [page-name (.getName page)
        title-part-of-name
        (if (maybe-extract-date-from-filename page config)
          (subs page-name (count (:post-date-format config)))
          page-name)
        h1 (first (filter #(= (:tag %) :h1) dom))]
    (or
     (:content h1)
     (capitalize (replace title-part-of-name #"[-_]+" " ")))))

(defn infer-date
  "The date is to be inferred from
   1. the prefix of the basename of the file, if it matches `dddd-dd-dd`; or
   2. the creation date of the file, otherwise."
  [^java.io.File page config]
  (.format
   (java.text.SimpleDateFormat. ^String (:post-date-format config) (Locale/getDefault))
   (or
    (maybe-extract-date-from-filename page config)
    (Date. (.toMillis (file-attribute page "creationTime"))))))

(defn infer-author
  "Infer the ordinary everyday name of the author of this `page`, given this 
   `config`."
  [^java.io.File page config]
  (or
   ;; this isn't good enough because so far it's only getting the username of
   ;; the author; we need a platform independent way of resolving the real name,
   ;; and so far I don't have that.
   (try (-> (Files/getFileAttributeView (.toPath page)
                                        FileOwnerAttributeView
                                        (into-array LinkOption []))
            .getOwner
            .getName)
        (catch Exception _ nil))
   (:author config)))

(defn infer-meta
  "Infer metadata related to this `page`, assumed to be the name of a markdown file"
  [^java.io.File page config markup]
  (with-open [rdr (java.io.PushbackReader. (reader page))]
    (let [dom (trimmed-html-snippet ((render-fn markup) rdr config))]
      (assoc {}
             :title (infer-title page config dom)
             :date (infer-date page config)
             :author (infer-author page config)))))