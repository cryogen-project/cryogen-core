(ns cryogen-core.compiler
  (:require [selmer.parser :refer [cache-off! render-file]]
            [selmer.util :refer [set-custom-resource-path!]]
            [io.aviso.exception :refer [write-exception]]
            [clojure.java.io :refer [copy file reader writer]]
            [clojure.string :as s]
            [text-decoration.core :refer :all]
            [pl.danieljanus.tagsoup :as tagsoup]
            [hiccup.core :as hiccup]
            [cryogen-core.toc :refer [generate-toc]]
            [cryogen-core.sass :as sass]
            [cryogen-core.markup :as m]
            [cryogen-core.io :refer
             [get-resource find-assets create-folder create-file wipe-public-folder
              copy-resources copy-resources-from-theme path]]
            [cryogen-core.sitemap :as sitemap]
            [cryogen-core.rss :as rss])
  (:import java.util.Locale))

(cache-off!)

(defn root-uri
  "Creates the uri for posts and pages. Returns root-path by default"
  [k config]
  (if-let [uri (k config)]
    uri
    (config (-> k (name) (s/replace #"-uri$" "") (keyword)))))

(defn re-pattern-from-ext
  "Creates a properly quoted regex pattern for the given file extension"
  [ext]
  (re-pattern (str (s/replace ext "." "\\.") "$")))

(defn find-posts
  "Returns a list of markdown files representing posts under the post root in templates/md"
  [{:keys [post-root ignored-files]} mu]
  (find-assets (path "templates" (m/dir mu) post-root) (m/ext mu) ignored-files))

(defn find-pages
  "Returns a list of markdown files representing pages under the page root in templates/md"
  [{:keys [page-root ignored-files]} mu]
  (find-assets (path "templates" (m/dir mu) page-root) (m/ext mu) ignored-files))

(defn parse-post-date
  "Parses the post date from the post's file name and returns the corresponding java date object"
  [^String file-name date-fmt]
  (let [fmt (java.text.SimpleDateFormat. date-fmt)]
    (.parse fmt (.substring file-name 0 10))))

(defn post-uri
  "Creates a post uri from the post file name"
  [file-name {:keys [blog-prefix post-root-uri]} mu]
  (path "/" blog-prefix post-root-uri (s/replace file-name (re-pattern-from-ext (m/ext mu)) ".html")))

(defn page-uri
  "Creates a page uri from the page file name"
  [page-name {:keys [blog-prefix page-root-uri]} mu]
  (path "/" blog-prefix page-root-uri (s/replace page-name (re-pattern-from-ext (m/ext mu)) ".html")))

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
  [^java.io.File page config markup]
  (with-open [rdr (java.io.PushbackReader. (reader page))]
    (let [page-name (.getName page)
          file-name (s/replace page-name (re-pattern-from-ext (m/ext markup)) ".html")
          page-meta (read-page-meta page-name rdr)
          content ((m/render-fn markup) rdr config)]
      {:file-name file-name
       :page-meta page-meta
       :content   content})))

(defn merge-meta-and-content
  "Merges the page metadata and content maps, adding :toc if necessary."
  [file-name page-meta content]
  (merge
    (update-in page-meta [:layout] #(str (name %) ".html"))
    {:file-name file-name
     :content   content
     :toc       (if-let [toc (:toc page-meta)]
                  (generate-toc content :list-type toc))}))

(defn parse-page
  "Parses a page/post and returns a map of the content, uri, date etc."
  [page config markup]
  (let [{:keys [file-name page-meta content]} (page-content page config markup)]
    (merge
      (merge-meta-and-content file-name page-meta content)
      {:uri        (page-uri file-name config markup)
       :page-index (:page-index page-meta)})))

(defn parse-post
  "Return a map with the given post's information."
  [page config markup]
  (let [{:keys [file-name page-meta content]} (page-content page config markup)]
    (merge
      (merge-meta-and-content file-name page-meta content)
      (let [date (if (:date page-meta)
                   (.parse (java.text.SimpleDateFormat. (:post-date-format config)) (:date page-meta))
                   (parse-post-date file-name (:post-date-format config)))
            archive-fmt (java.text.SimpleDateFormat. "yyyy MMMM" (Locale/getDefault))
            formatted-group (.format archive-fmt date)]
        {:date                    date
         :formatted-archive-group formatted-group
         :parsed-archive-group    (.parse archive-fmt formatted-group)
         :uri                     (post-uri file-name config markup)
         :tags                    (set (:tags page-meta))}))))

(defn read-posts
  "Returns a sequence of maps representing the data from markdown files of posts.
   Sorts the sequence by post date."
  [config]
  (->> (mapcat
         (fn [mu]
           (->>
             (find-posts config mu)
             (pmap #(parse-post % config mu))
             (remove #(= (:draft? %) true))))
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
            (update-in tags [tag] (fnil conj []) (select-keys post [:uri :title :content :date :enclosure])))
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
  [{:keys [blog-prefix tag-root-uri]} tag]
  {:name (name tag)
   :uri  (path "/" blog-prefix tag-root-uri (str (name tag) ".html"))})

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
  [{:keys [blog-prefix page-root-uri] :as params} pages]
  (when-not (empty? pages)
    (println (blue "compiling pages"))
    (create-folder (path "/" blog-prefix page-root-uri))
    (doseq [{:keys [uri] :as page} pages]
      (println "\t-->" (cyan uri))
      (create-file uri
                   (render-file (str "/html/" (:layout page))
                                (merge params
                                       {:active-page     "pages"
                                        :servlet-context (path "/" blog-prefix "/")
                                        :page            page
                                        :uri             uri}))))))

(defn compile-posts
  "Compiles all the posts into html and spits them out into the public folder"
  [{:keys [blog-prefix post-root-uri disqus-shortname] :as params} posts]
  (when-not (empty? posts)
    (println (blue "compiling posts"))
    (create-folder (path "/" blog-prefix post-root-uri))
    (doseq [post posts]
      (println "\t-->" (cyan (:uri post)))
      (create-file (:uri post)
                   (render-file (str "/html/" (:layout post))
                                (merge params
                                       {:active-page      "posts"
                                        :servlet-context  (path "/" blog-prefix "/")
                                        :post             post
                                        :disqus-shortname disqus-shortname
                                        :uri              (:uri post)}))))))

(defn compile-tags
  "Compiles all the tag pages into html and spits them out into the public folder"
  [{:keys [blog-prefix tag-root-uri] :as params} posts-by-tag]
  (when-not (empty? posts-by-tag)
    (println (blue "compiling tags"))
    (create-folder (path "/" blog-prefix tag-root-uri))
    (doseq [[tag posts] posts-by-tag]
      (let [{:keys [name uri]} (tag-info params tag)]
        (println "\t-->" (cyan uri))
        (create-file uri
                     (render-file "/html/tag.html"
                                  (merge params
                                         {:active-page     "tags"
                                          :servlet-context (path "/" blog-prefix "/")
                                          :name            name
                                          :posts           posts
                                          :uri             uri})))))))

(defn compile-tags-page [{:keys [blog-prefix] :as params}]
  (println (blue "compiling tags page"))
  (create-file (path "/"  blog-prefix "tags.html")
               (render-file "/html/tags.html"
                            (merge params
                                   {:active-page "tags"
                                    :uri         (path "/" blog-prefix "tags.html")}))))

(defn content-until-more-marker
  [^String content]
  (let [index (.indexOf content "<!--more-->")]
    (if (pos? index)
      (let [s (subs content 0 index)]
        (->> ((tagsoup/parse-string s) 2)
             (drop 2)
             hiccup/html)))))

(defn create-preview
  "Creates a single post preview"
  [blocks-per-preview post]
  (merge post
         {:content (or (content-until-more-marker (:content post))
                       (->> ((tagsoup/parse-string (:content post)) 2)
                            (drop 2)
                            (take blocks-per-preview)
                            hiccup/html))}))

(defn create-previews
  "Returns a sequence of vectors, each containing a set of post previews"
  [posts-per-page blocks-per-preview posts]
  (->> posts
       (map #(create-preview blocks-per-preview %))
       (partition-all posts-per-page)
       (map-indexed (fn [i v] {:index (inc i) :posts v}))))

(defn create-preview-links
  "Turn each vector of previews into a map with :prev and :next keys that contain the uri of the
  prev/next preview page"
  [previews blog-prefix]
  (mapv (fn [[prev target next]]
          (merge target
                 {:prev (if prev (path "/" blog-prefix "p" (str (:index prev) ".html")) nil)
                  :next (if next (path "/" blog-prefix "p" (str (:index next) ".html")) nil)}))
        (partition 3 1 (flatten [nil previews nil]))))

(defn compile-preview-pages
  "Compiles a series of pages containing 'previews' from each post"
  [{:keys [blog-prefix posts-per-page blocks-per-preview] :as params} posts]
  (when-not (empty? posts)
    (let [previews (-> (create-previews posts-per-page blocks-per-preview posts)
                       (create-preview-links blog-prefix))
          previews (if (> (count previews) 1) (assoc-in previews [1 :prev] (path "/" blog-prefix "index.html")) previews)]
      (create-folder (path "/" blog-prefix "p"))
      (doseq [{:keys [index posts prev next]} previews
              :let [index-page? (= 1 index)]]
        (create-file (if index-page? (path "/" blog-prefix "index.html") (path "/" blog-prefix "p" (str index ".html")))
                     (render-file "/html/previews.html"
                                  (merge params
                                         {:active-page     "preview"
                                          :home            (when index-page? true)
                                          :servlet-context (path "/" blog-prefix "/")
                                          :posts           posts
                                          :prev-uri        prev
                                          :next-uri        next})))))))

(defn compile-index
  "Compiles the index page into html and spits it out into the public folder"
  [{:keys [blog-prefix disqus?] :as params}]
  (println (blue "compiling index"))
  (create-file (path "/" blog-prefix "index.html")
               (render-file "/html/home.html"
                            (merge params
                                   {:active-page "home"
                                    :home        true
                                    :disqus?     disqus?
                                    :post        (get-in params [:latest-posts 0])
                                    :uri         (path "/" blog-prefix "index.html")}))))

(defn compile-archives
  "Compiles the archives page into html and spits it out into the public folder"
  [{:keys [blog-prefix] :as params} posts]
  (println (blue "compiling archives"))
  (create-file (path "/" blog-prefix "archives.html")
        (render-file "/html/archives.html"
                     (merge params
                            {:active-page "archives"
                             :archives    true
                             :groups      (group-for-archive posts)
                             :uri         (path "/" blog-prefix "/archives.html")}))))

(defn tag-posts
  "Converts the tags in each post into links"
  [posts config]
  (map #(update-in % [:tags] (partial map (partial tag-info config))) posts))

(defn copy-resources-from-markup-folders
  "Copy resources from markup folders"
  [{:keys [post-root page-root] :as config}]
  (copy-resources
    (merge config
           {:resources     (for [mu (m/markups)
                                 t (distinct [post-root page-root])] (str (m/dir mu) "/" t))
            :ignored-files (map #(re-pattern-from-ext (m/ext %)) (m/markups))})))

(defn read-config
  "Reads the config file"
  []
  (try
    (let [config (-> "templates/config.edn"
                     get-resource
                     slurp
                     read-string
                     (update-in [:blog-prefix] (fnil str ""))
                     (update-in [:page-root] (fnil str ""))
                     (update-in [:post-root] (fnil str ""))
                     (update-in [:tag-root-uri] (fnil str ""))
                     (update-in [:rss-name] (fnil str "rss.xml"))
                     (update-in [:rss-filters] (fnil seq []))
                     (update-in [:sass-src] (fnil str "css"))
                     (update-in [:sass-dest] (fnil str "css"))
                     (update-in [:post-date-format] (fnil str "yyyy-MM-dd"))
                     (update-in [:keep-files] (fnil seq []))
                     (update-in [:ignored-files] (fnil seq [#"^\.#.*" #".*\.swp$"])))]
      (merge
        config
        {:page-root-uri (root-uri :page-root-uri config)
         :post-root-uri (root-uri :post-root-uri config)}))
    (catch Exception _
      (throw (IllegalArgumentException. "Failed to parse config.edn")))))

(defn compile-assets
  "Generates all the html and copies over resources specified in the config"
  []
  (println (green "compiling assets..."))
  (let [{:keys [^String site-url blog-prefix rss-name recent-posts sass-src sass-dest keep-files ignored-files previews?] :as config} (read-config)
        posts (add-prev-next (read-posts config))
        pages (add-prev-next (read-pages config))
        [navbar-pages sidebar-pages] (group-pages pages)
        posts-by-tag (group-by-tags posts)
        posts (tag-posts posts config)
        params (merge config
                      {:title         (:site-title config)
                       :active-page   "home"
                       :tags          (map (partial tag-info config) (keys posts-by-tag))
                       :latest-posts  (->> posts (take recent-posts) vec)
                       :navbar-pages  navbar-pages
                       :sidebar-pages sidebar-pages
                       :archives-uri  (path "/" blog-prefix "archives.html")
                       :index-uri     (path "/" blog-prefix "index.html")
                       :tags-uri      (path "/" blog-prefix "tags.html")
                       :rss-uri       (path "/" blog-prefix rss-name)
                       :site-url      (if (.endsWith site-url "/") (.substring site-url 0 (dec (count site-url))) site-url)
                       :theme-path    (str "file:resources/templates/themes/" (:theme config))})]

    (set-custom-resource-path! (:theme-path params))
    (wipe-public-folder keep-files)
    (println (blue "copying theme resources"))
    (copy-resources-from-theme config)
    (println (blue "copying resources"))
    (copy-resources config)
    (copy-resources-from-markup-folders config)
    (compile-pages params pages)
    (compile-posts params posts)
    (compile-tags params posts-by-tag)
    (compile-tags-page params)
    (if previews?
      (compile-preview-pages params posts)
      (compile-index params))
    (compile-archives params posts)
    (println (blue "generating site map"))
    (create-file (path "/" blog-prefix "sitemap.xml") (sitemap/generate site-url ignored-files))
    (println (blue "generating main rss"))
    (create-file (path "/" blog-prefix rss-name) (rss/make-channel config posts))
    (println (blue "generating filtered rss"))
    (rss/make-filtered-channels config posts-by-tag)
    (println (blue "compiling sass"))
    (sass/compile-sass->css!
      {:src-sass      sass-src
       :dest-sass     (path ".." "public" blog-prefix sass-dest)
       :ignored-files ignored-files
       :base-dir      "resources/templates/"})))

(defn compile-assets-timed []
  (time
    (try
      (compile-assets)
      (catch Exception e
        (if (or (instance? IllegalArgumentException e)
                (instance? clojure.lang.ExceptionInfo e))
          (println (red "Error:") (yellow (.getMessage e)))
          (write-exception e))))))
