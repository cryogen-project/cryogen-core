(ns cryogen-core.compiler
  (:require [selmer.parser :refer [cache-off! render-file]]
            [cryogen-core.io :refer
             [get-resource find-assets create-folder wipe-public-folder copy-resources
              copy-images-from-markdown-folders]]
            [cryogen-core.sitemap :as sitemap]
            [cryogen-core.rss :as rss]
            [io.aviso.exception :refer [write-exception]]
            [clojure.java.io :refer [copy file reader writer]]
            [clojure.string :as s]
            [text-decoration.core :refer :all]
            [cryogen-core.toc :refer [generate-toc]]
            [cryogen-core.sass :as sass]
            [cryogen-core.markup :as m]))

(cache-off!)

(def public "resources/public")

(defn root-path
  "Creates the root path for posts, tags and pages"
  [config k]
  (if-let [root (k config)]
    (str "/" root "/") "/"))

(defn re-pattern-from-ext
  "Creates a properly quoted regex pattern for the given file extension"
  [ext]
  (re-pattern (str (s/replace ext "." "\\.") "$")))

(defn find-md-assets
  "Returns a list of files ending with .md under templates"
  [ignored-files]
  (find-assets "templates" ".md" ignored-files))

(defn find-posts
  "Returns a list of markdown files representing posts under the post root in templates/md"
  [{:keys [post-root ignored-files]} mu]
  (find-assets (str "templates/" (m/dir mu) post-root) (m/ext mu) ignored-files))

(defn find-pages
  "Returns a list of markdown files representing pages under the page root in templates/md"
  [{:keys [page-root ignored-files]} mu]
  (find-assets (str "templates/" (m/dir mu) page-root) (m/ext mu) ignored-files))

(defn parse-post-date
  "Parses the post date from the post's file name and returns the corresponding java date object"
  [file-name date-fmt]
  (let [fmt (java.text.SimpleDateFormat. date-fmt)]
    (.parse fmt (.substring file-name 0 10))))

(defn post-uri
  "Creates a post uri from the post file name"
  [file-name {:keys [blog-prefix post-root]} mu]
  (str blog-prefix post-root (s/replace file-name (re-pattern-from-ext (m/ext mu)) ".html")))

(defn page-uri
  "Creates a page uri from the page file name"
  [page-name {:keys [blog-prefix page-root]} mu]
  (str blog-prefix page-root (s/replace page-name (re-pattern-from-ext (m/ext mu)) ".html")))

(defn read-page-meta
  "Returns the clojure map from the top of a markdown page/post"
  [page rdr]
  (try
    (read rdr)
    (catch Exception _
      (throw (IllegalArgumentException. (str "Malformed metadata on page: " page))))))

(defn page-content
  "Returns a map with the given page's file-name, metadata and content parsed from
  the file with the given markup."
  [page config markup]
  (with-open [rdr (java.io.PushbackReader. (reader page))]
    (let [page-name (.getName page)
          file-name (s/replace page-name (re-pattern-from-ext (m/ext markup)) ".html")
          page-meta (read-page-meta page-name rdr)
          content ((m/render-fn markup) rdr config)]
      {:file-name file-name
       :page-meta page-meta
       :content content})))

(defn merge-meta-and-content
  "Merges the page metadata and content maps, adding :toc if necessary."
  [file-name page-meta content]
  (merge
   (update-in page-meta [:layout] #(str (name %) ".html"))
   {:file-name file-name
    :content   content
    :toc       (if (:toc page-meta) (generate-toc content))}))

(defn parse-page
  "Parses a page/post and returns a map of the content, uri, date etc."
  [page config markup]
  (let [{:keys [file-name page-meta content]} (page-content page config markup)]
    (merge
     (merge-meta-and-content file-name page-meta content)
     {:uri (page-uri file-name config markup)
      :page-index (:page-index page-meta)})))

(defn parse-post
  "Return a map with the given post's information."
  [page config markup]
  (let [{:keys [file-name page-meta content]} (page-content page config markup)]
    (merge
     (merge-meta-and-content file-name page-meta content)
     (let [date (parse-post-date file-name (:post-date-format config))
           archive-fmt (java.text.SimpleDateFormat. "yyyy MMMM" (java.util.Locale. "en"))
           formatted-group (.format archive-fmt date)]
       {:date                    date
        :formatted-archive-group formatted-group
        :parsed-archive-group    (.parse archive-fmt formatted-group)
        :uri                     (post-uri file-name config markup)
        :tags                    (set (:tags page-meta))})
     )))

(defn read-posts
  "Returns a sequence of maps representing the data from markdown files of posts.
   Sorts the sequence by post date."
  [config]
  (->> (mapcat
        (fn [mu]
          (->>
           (find-posts config mu)
           (map #(parse-post % config mu))))
        (m/markups))
       (sort-by :date)
       reverse))

(defn read-pages
  "Returns a sequence of maps representing the data from markdown files of pages.
  Sorts the sequence by post date."
  [config]
  (->> (mapcat
        (fn [mu]
          (->>
           (find-pages config mu)
           (map #(parse-page % config mu))))
        (m/markups))
       (sort-by :page-index)))

(defn tag-post
  "Adds the uri and title of a post to the list of posts under each of its tags"
  [tags post]
  (reduce (fn [tags tag]
            (update-in tags [tag] (fnil conj []) (select-keys post [:uri :title :content])))
          tags (:tags post)))

(defn group-by-tags
  "Maps all the tags with a list of posts that contain each tag"
  [posts]
  (reduce tag-post {} posts))

(defn group-for-archive
  "Groups the posts by month and year for archive sorting"
  [posts]
  (->> posts
       (map #(select-keys % [:title :uri :date :formatted-archive-group :parsed-archive-group]))
       (group-by :formatted-archive-group)
       (map (fn [[group posts]]
              {:group        group
               :parsed-group (:parsed-archive-group (get posts 0))
               :posts        (map #(select-keys % [:title :uri :date]) posts)}))
       (sort-by :parsed-group)
       reverse))

(defn tag-info
  "Returns a map containing the name and uri of the specified tag"
  [{:keys [blog-prefix tag-root]} tag]
  {:name (name tag)
   :uri  (str blog-prefix tag-root (name tag) ".html")})

(defn add-prev-next
  "Adds a :prev and :next key to the page/post data containing the title and uri of the prev/next
  post/page if it exists"
  [pages]
  (map (fn [[prev target next]]
         (assoc target
                :prev (if prev (select-keys prev [:title :uri]) nil)
                :next (if next (select-keys next [:title :uri]) nil)))
       (partition 3 1 (flatten [nil pages nil]))))

(defn group-pages
  "Separates the pages into links for the navbar and links for the sidebar"
  [pages]
  (let [{navbar-pages  true
         sidebar-pages false} (group-by #(boolean (:navbar? %)) pages)]
    (map (partial sort-by :page-index) [navbar-pages sidebar-pages])))

(defn compile-pages
  "Compiles all the pages into html and spits them out into the public folder"
  [{:keys [blog-prefix page-root] :as params} pages]
  (when-not (empty? pages)
    (println (blue "compiling pages"))
    (create-folder (str blog-prefix page-root))
    (doseq [{:keys [uri] :as page} pages]
      (println "\t-->" (cyan uri))
      (spit (str public uri)
            (render-file "templates/html/layouts/page.html"
                         (merge params
                                {:servlet-context "../"
                                 :page            page
                                 :uri             uri}))))))

(defn compile-posts
  "Compiles all the posts into html and spits them out into the public folder"
  [{:keys [blog-prefix post-root disqus-shortname] :as params} posts]
  (when-not (empty? posts)
    (println (blue "compiling posts"))
    (create-folder (str blog-prefix post-root))
    (doseq [post posts]
      (println "\t-->" (cyan (:uri post)))
      (spit (str public (:uri post))
            (render-file (str "templates/html/layouts/" (:layout post))
                         (merge params
                                {:servlet-context  "../"
                                 :post             post
                                 :disqus-shortname disqus-shortname
                                 :uri              (:uri post)}))))))

(defn compile-tags
  "Compiles all the tag pages into html and spits them out into the public folder"
  [{:keys [blog-prefix tag-root] :as params} posts-by-tag]
  (when-not (empty? posts-by-tag)
    (println (blue "compiling tags"))
    (create-folder (str blog-prefix tag-root))
    (doseq [[tag posts] posts-by-tag]
      (let [{:keys [name uri]} (tag-info params tag)]
        (println "\t-->" (cyan uri))
        (spit (str public uri)
              (render-file "templates/html/layouts/tag.html"
                           (merge params
                                  {:servlet-context "../"
                                   :name            name
                                   :posts           posts
                                   :uri             uri})))))))

(defn compile-index
  "Compiles the index page into html and spits it out into the public folder"
  [{:keys [blog-prefix disqus?] :as params}]
  (println (blue "compiling index"))
  (spit (str public blog-prefix "/index.html")
        (render-file "templates/html/layouts/home.html"
                     (merge params
                            {:home    true
                             :disqus? disqus?
                             :post    (get-in params [:latest-posts 0])
                             :uri     (str blog-prefix "/index.html")}))))

(defn compile-archives
  "Compiles the archives page into html and spits it out into the public folder"
  [{:keys [blog-prefix] :as params} posts]
  (println (blue "compiling archives"))
  (spit (str public blog-prefix "/archives.html")
        (render-file "templates/html/layouts/archives.html"
                     (merge params
                            {:archives true
                             :groups   (group-for-archive posts)
                             :uri      (str blog-prefix "/archives.html")}))))

(defn tag-posts
  "Converts the tags in each post into links"
  [posts config]
  (map #(update-in % [:tags] (partial map (partial tag-info config))) posts))

(defn read-config
  "Reads the config file"
  []
  (try
    (let [config (-> "templates/config.edn"
                     get-resource
                     slurp
                     read-string
                     (update-in [:blog-prefix] (fnil str ""))
                     (update-in [:rss-name] (fnil str "rss.xml"))
                     (update-in [:rss-filters] (fnil seq []))
                     (update-in [:sass-src] (fnil str "css"))
                     (update-in [:sass-dest] (fnil str "css"))
                     (update-in [:post-date-format] (fnil str "yyyy-MM-dd"))
                     (update-in [:keep-files] (fnil seq []))
                     (update-in [:ignored-files] (fnil seq [#"^\.#.*" #".*\.swp$"])))]
      (merge
       config
       {:page-root (root-path :page-root config)
        :post-root (root-path :post-root config)
        :tag-root  (root-path :tag-root config)}))
    (catch Exception _
      (throw (IllegalArgumentException. "Failed to parse config.edn")))))

(defn compile-assets
  "Generates all the html and copies over resources specified in the config"
  []
  (println (green "compiling assets..."))
  (let [{:keys [site-url blog-prefix rss-name recent-posts sass-src sass-dest keep-files ignored-files] :as config} (read-config)
        posts (add-prev-next (read-posts config))
        pages (add-prev-next (read-pages config))
        [navbar-pages sidebar-pages] (group-pages pages)
        posts-by-tag (group-by-tags posts)
        posts (tag-posts posts config)
        params (merge config
                      {:title         (:site-title config)
                       :tags          (map (partial tag-info config) (keys posts-by-tag))
                       :latest-posts  (->> posts (take recent-posts) vec)
                       :navbar-pages  navbar-pages
                       :sidebar-pages sidebar-pages
                       :archives-uri  (str blog-prefix "/archives.html")
                       :index-uri     (str blog-prefix "/index.html")
                       :rss-uri       (str blog-prefix "/" rss-name)
                       :site-url      (if (.endsWith site-url "/") (.substring site-url 0 (dec (count site-url))) site-url)})]

    (wipe-public-folder keep-files)
    (println (blue "copying resources"))
    (copy-resources config)
    (copy-images-from-markdown-folders config)
    (compile-pages params pages)
    (compile-posts params posts)
    (compile-tags params posts-by-tag)
    (compile-index params)
    (compile-archives params posts)
    (println (blue "generating site map"))
    (spit (str public blog-prefix "/sitemap.xml") (sitemap/generate site-url ignored-files))
    (println (blue "generating main rss"))
    (spit (str public blog-prefix "/" rss-name) (rss/make-channel config posts))
    (println (blue "generating filtered rss"))
    (rss/make-filtered-channels public config posts-by-tag)
    (println (blue "compiling sass"))
    (sass/compile-sass->css!
     {:src-sass sass-src
      :dest-sass (str "../public" blog-prefix "/" sass-dest)
      :ignored-files ignored-files
      :base-dir "resources/templates/"})))

(defn compile-assets-timed []
  (time
    (try
      (compile-assets)
      (catch Exception e
        (if (or (instance? IllegalArgumentException e)
                (instance? clojure.lang.ExceptionInfo e))
          (println (red "Error:") (yellow (.getMessage e)))
          (write-exception e))))))
