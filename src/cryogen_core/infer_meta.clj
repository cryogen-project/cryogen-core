(ns cryogen-core.infer-meta
  (:refer-clojure :exclude [replace])
  (:require [clojure.java.io :refer [reader]]
            [clojure.string :refer [capitalize join lower-case replace
                                    split starts-with? trim]]
            [cryogen-core.console-message :refer [error info warn]]
            [cryogen-core.io :refer [get-resource]]
            [cryogen-core.markup :refer [exts render-fn]]
            [cryogen-core.util :refer [enlive->plain-text parse-post-date
                                       re-pattern-from-exts
                                       trimmed-html-snippet]]
            [mikera.image.core :refer [height load-image width]]
            [pantomime.mime :refer [mime-type-of]]
            [cc.journeyman.real-name.core :refer [get-real-name]])
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

(defn- maybe-extract-date-from-filename
  [page config]
  (try (parse-post-date (.getName page) (:post-date-format config))
       (catch Exception _
         (warn (format "Failed to extract date from filename `%s`." (.getName page)))
         nil)))

(defmacro walk-dom
  "Return a flat sequence of all elements in this `dom` structure, recursively."
  [dom]
  `(tree-seq map? :content {:content ~dom}))

(defn infer-image-data
  "Infer image data given this `dom` representation."
  [^java.io.File dom config]
  (let [img (first
             (filter
              #(= (:tag %) :img)
              (walk-dom dom)))
        src (when img (-> img :attrs :src))
        prefix (:blog-prefix config)
        path (when src (if (starts-with? src prefix)
                         (subs src (count prefix))
                         src))
        ;; put paths are actually relative to the content directory
        content-path (when path (str "content" path))
        image (when path (try
                           (load-image (get-resource content-path))
                           (catch Exception e
                             (warn (format "Image `%s` was not found." content-path))
                             (error e)
                             nil)))
        alt (when img (-> img :attrs :alt))]
    (if image
      (let [h (when image (height image))
            w (when image (width image))
            mime (when image
                   (try
                     (mime-type-of content-path)
                     (catch Throwable e
                       (warn (format "Could not detect mime type of `%s`." content-path))
                       (error e)
                       nil)))]
        (assoc {} :path path
               :alt alt
               :width w
               :height h
               :type mime))
      path)))

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

(defn main-title
  "Return the content of the first `h1` element of this `dom`, if any."
  [dom]
  (first (filter #(= (:tag %) :h1) (walk-dom dom))))

(defn main-title?
  "`true` if this `elt` is the main title taken from this `dom`."
  [elt dom]
  (= elt (main-title dom)))

;; expecting {:tag :strong, :attrs nil, :content ("Tags:")}

(defn tag-line?
  "`true` if this `elt` taken from a dom is a line starting with a strongly-emphasised
   string `Tag:`?"
  [elt]
  (let [first-elt (first (:content elt))]
    (and (= (:tag elt) :p)
         (map? first-elt)
         (= (:tag first-elt) :strong)
         (string? (first (:content first-elt)))
         (starts-with? (enlive->plain-text (list first-elt)) "Tags:"))))

(defn- redundant?
  "True if this `elt` is redundant within this `dom` once meta-data has been 
   extracted."
  [elt dom]
  (or (main-title? elt dom)
      (tag-line? elt)))

;; OK, the right way to do this is obviously with clojure.walk, but as I'm too
;; stupid to understand that I'll have to do it recursively. A dom, in this
;; context, is a sequence of maps, each of which may have a :content key
;; whose value is a sequence of strings and/or maps.
(defn clean
  "Return a dom like this `dom` but with those elements which (could) have 
   been used to extract meta-data removed."
  ([dom] (clean dom dom))
  ([elt dom]
  (cond
    (map? elt) (when-not (redundant? elt dom) (assoc elt :content (clean (:content elt) dom)))
    (coll? elt) (remove nil? (map #(clean % dom) elt))
    :else elt)))

(def infer-title
  "Infer the title of this page, ideally by extracting the first `H1` element from this
   `dom` (Document Object Model) of its content, given this `config`."
  ;; dom turns out to be a list of maps; I'm not yet certain what happens when elements
  ;; have child elements but for the time being it doesn't matter.
  (memoize
   (fn [^java.io.File page config dom]
     (let [page-name (.getName page)
           title-part-of-name
           (if (maybe-extract-date-from-filename page config)
             (subs page-name (count (:post-date-format config)))
             page-name)
           h1 (main-title dom)]
       (if h1
         (enlive->plain-text (:content h1))
         (capitalize
          (trim
           (replace
            (replace title-part-of-name #"[-_]+" " ")
            #"\.[a-z]+$" ""))))))))

(defn infer-description
  [^java.io.File page config dom]
  (let [p (first (filter #(= (:tag %) :p)
                         (walk-dom dom)))]
    (if p (enlive->plain-text (:content p))
        (infer-title page config dom))))

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
   (try (get-real-name (-> (Files/getFileAttributeView (.toPath page)
                                                       FileOwnerAttributeView
                                                       (into-array LinkOption []))
                           .getOwner
                           .getName))
        (catch Exception e
          (error e)
          nil))
   (:author config)))

(defn infer-tags
  "Return a sequence of all tags found in this `dom`."
  [dom]
  (let [tags-p (filter
                tag-line?
                (walk-dom dom))
        tags (when tags-p (join ", " (reduce concat (map #(rest (:content %)) tags-p))))]
    (when tags (doall (set (map trim (split tags #",")))))))

(defn infer-meta
  "Infer metadata related to this `page`, assumed to be the name of a file in 
   this `markup`, given this `config`."
  [^java.io.File page config dom]
    (let [metadata (assoc {}
                          :author (infer-author page config)
                          :date (infer-date page config)
                          :description (infer-description page config dom)
                          :image (infer-image-data dom config)
                          :inferred-meta true
                          :tags (infer-tags dom)
                          :title (infer-title page config dom))]
      (info (format "Inferred metadata for document %s dated %s."
                    (:title metadata)
                    (:date metadata)))
      metadata))

(defn using-inferred-metadata
  "An implementation of the guts of `cryogen-core.compiler.page-content` for
   pages without embedded metadata. Read this `page` in this `markup`, given 
   this `config` and, it possible, return a map with the keys `:file-name`, 
   `:page-meta`, `:content-dom` where the value of `:page-meta` is appropriate
   meta-data inferred from the content of the page."
  [page markup config]
  (with-open [rdr (java.io.PushbackReader. (reader page))]
    (let [content-dom (trimmed-html-snippet ((render-fn markup) rdr config))
          page-meta (infer-meta page config content-dom)
          file-name (infer-file-name page page-meta config)]
      {:file-name   file-name
       :page-meta   page-meta
       :content-dom (clean content-dom)})))
