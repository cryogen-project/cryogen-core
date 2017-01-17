(ns cryogen-core.compiler
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as s]
            [io.aviso.exception :refer [write-exception]]
            [net.cgrand.enlive-html :as enlive]
            [selmer.parser :refer [cache-off! render-file]]
            [selmer.util :refer [set-custom-resource-path!]]
            [text-decoration.core :refer :all]
            [cryogen-core.io :as cryogen-io]
            [cryogen-core.markup :as m]
            [cryogen-core.rss :as rss]
            [cryogen-core.sass :as sass]
            [cryogen-core.sitemap :as sitemap]
            [cryogen-core.toc :as toc])
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

(defn find-entries
  "Returns a list of files under the templates directory according to the
  implemented Markup protocol and specified root directory. It defaults to
  looking under the implemented protocol's subdirectory, but fallsback to look
  at the templates directory."
  [root mu ignored-files]
  (let [assets (cryogen-io/find-assets
                 (cryogen-io/path "templates" (m/dir mu) root)
                 (m/ext mu)
                 ignored-files)]
    (if (seq assets)
      assets
      (cryogen-io/find-assets
        (cryogen-io/path "templates" root)
        (m/ext mu)
        ignored-files))))

(defn find-posts
  "Returns a list of markdown files representing posts under the post root."
  [{:keys [post-root ignored-files]} mu]
  (find-entries post-root mu ignored-files))

(defn find-pages
  "Returns a list of markdown files representing pages under the page root."
  [{:keys [page-root ignored-files]} mu]
  (find-entries page-root mu ignored-files))

(defn parse-post-date
  "Parses the post date from the post's file name and returns the corresponding java date object"
  [^String file-name date-fmt]
  (let [fmt (java.text.SimpleDateFormat. date-fmt)]
    (.parse fmt (.substring file-name 0 10))))

(defn page-uri
  "Creates a URI from file name. `uri-type` is any of the uri types specified in config, e.g., `:post-root-uri`."
  ([file-name params]
   (page-uri file-name nil params))
  ([file-name uri-type {:keys [blog-prefix clean-urls?] :as params}]
   (let [page-uri (get params uri-type)
         uri-end  (if clean-urls? (s/replace file-name #"(index)?\.html" "/") file-name)]
     (cryogen-io/path "/" blog-prefix page-uri uri-end))))

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
  (with-open [rdr (java.io.PushbackReader. (io/reader page))]
    (let [re-root   (re-pattern (str "^.*?(" (:page-root config) "|" (:post-root config) ")/"))
          page-fwd  (s/replace (str page) "\\" "/")         ;; make it work on Windows
          page-name (s/replace page-fwd re-root "")
          file-name (s/replace page-name (re-pattern-from-ext (m/ext markup)) ".html")
          page-meta (read-page-meta page-name rdr)
          content   ((m/render-fn markup) rdr config)]
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
                  (toc/generate-toc content :list-type toc))}))

(defn parse-page
  "Parses a page/post and returns a map of the content, uri, date etc."
  [page config markup]
  (let [{:keys [file-name page-meta content]} (page-content page config markup)]
    (merge
      (merge-meta-and-content file-name page-meta content)
      {:uri        (page-uri file-name :page-root-uri config)
       :page-index (:page-index page-meta)})))

(defn parse-post
  "Return a map with the given post's information."
  [page config markup]
  (let [{:keys [file-name page-meta content]} (page-content page config markup)]
    (merge
      (merge-meta-and-content file-name page-meta content)
      (let [date            (if (:date page-meta)
                              (.parse (java.text.SimpleDateFormat. (:post-date-format config)) (:date page-meta))
                              (parse-post-date file-name (:post-date-format config)))
            archive-fmt     (java.text.SimpleDateFormat. (:archive-group-format config "yyyy MMMM") (Locale/getDefault))
            formatted-group (.format archive-fmt date)]
        {:date                    date
         :formatted-archive-group formatted-group
         :parsed-archive-group    (.parse archive-fmt formatted-group)
         :uri                     (page-uri file-name :post-root-uri config)
         :tags                    (set (:tags page-meta))}))))

(defn read-posts
  "Returns a sequence of maps representing the data from markdown files of posts.
   Sorts the sequence by post date."
  [config]
  (->> (m/markups)
       (mapcat
         (fn [mu]
           (->>
             (find-posts config mu)
             (pmap #(parse-post % config mu))
             (remove #(= (:draft? %) true)))))
       (sort-by :date)
       reverse
       (drop-while #(and (:hide-future-posts? config) (.after (:date %) (java.util.Date.))))))

(defn read-pages
  "Returns a sequence of maps representing the data from markdown files of pages.
  Sorts the sequence by post date."
  [config]
  (->> (m/markups)
       (mapcat
         (fn [mu]
           (->>
             (find-pages config mu)
             (map #(parse-page % config mu)))))
       (sort-by :page-index)))

(defn tag-post
  "Adds the uri and title of a post to the list of posts under each of its tags"
  [tags post]
  (reduce (fn [tags tag]
            (update-in tags [tag] (fnil conj []) (select-keys post [:uri :title :content :date :enclosure])))
          tags
          (:tags post)))

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

(defn group-for-author
  "Groups the posts by author. If no post author if found defaults `default-author`."
  [posts default-author]
  (->> posts
       (map #(select-keys % [:title :uri :date :formatted-archive-group :parsed-archive-group :author]))
       (map #(update % :author (fn [author] (or author default-author))))
       (group-by :author)
       (map (fn [[author posts]]
              {:author author
               :posts  posts}))))

(defn tag-info
  "Returns a map containing the name and uri of the specified tag"
  [config tag]
  {:name (name tag)
   :uri  (page-uri (str (name tag) ".html") :tag-root-uri config)})

(defn add-prev-next
  "Adds a :prev and :next key to the page/post data containing the metadata of the prev/next
  post/page if it exists"
  [pages]
  (map (fn [[prev target next]]
         (assoc target
           :prev (if prev (dissoc prev :content) nil)
           :next (if next (dissoc next :content) nil)))
       (partition 3 1 (flatten [nil pages nil]))))

(defn group-pages
  "Separates the pages into links for the navbar and links for the sidebar"
  [pages]
  (let [{navbar-pages  true
         sidebar-pages false} (group-by #(boolean (:navbar? %)) pages)]
    (map (partial sort-by :page-index) [navbar-pages sidebar-pages])))

(defn write-html
  "When `clean-urls?` is set, appends `/index.html` before spit; otherwise just spits."
  [file-uri {:keys [clean-urls?]} data]
  (if clean-urls?
    (cryogen-io/create-file-recursive (cryogen-io/path file-uri "index.html") data)
    (cryogen-io/create-file file-uri data)))

(defn- print-debug-info [data]
  (println "DEBUG:")
  (pprint data))

(defn compile-pages
  "Compiles all the pages into html and spits them out into the public folder"
  [{:keys [blog-prefix page-root-uri debug?] :as params} pages]
  (when-not (empty? pages)
    (println (blue "compiling pages"))
    (cryogen-io/create-folder (cryogen-io/path "/" blog-prefix page-root-uri))
    (doseq [{:keys [uri] :as page} pages]
      (println "-->" (cyan uri))
      (when debug?
        (print-debug-info page))
      (write-html uri
                  params
                  (render-file (str "/html/" (:layout page))
                               (merge params
                                      {:active-page     "pages"
                                       :home            false
                                       :servlet-context (cryogen-io/path "/" blog-prefix "/")
                                       :page            page
                                       :uri             uri}))))))

(defn compile-posts
  "Compiles all the posts into html and spits them out into the public folder"
  [{:keys [blog-prefix post-root-uri disqus-shortname debug?] :as params} posts]
  (when-not (empty? posts)
    (println (blue "compiling posts"))
    (cryogen-io/create-folder (cryogen-io/path "/" blog-prefix post-root-uri))
    (doseq [{:keys [uri] :as post} posts]
      (println "-->" (cyan uri))
      (when debug?
        (print-debug-info post))
      (write-html uri
                  params
                  (render-file (str "/html/" (:layout post))
                               (merge params
                                      {:active-page      "posts"
                                       :servlet-context  (cryogen-io/path "/" blog-prefix "/")
                                       :post             post
                                       :disqus-shortname disqus-shortname
                                       :uri              uri}))))))

(defn compile-tags
  "Compiles all the tag pages into html and spits them out into the public folder"
  [{:keys [blog-prefix tag-root-uri] :as params} posts-by-tag]
  (when-not (empty? posts-by-tag)
    (println (blue "compiling tags"))
    (cryogen-io/create-folder (cryogen-io/path "/" blog-prefix tag-root-uri))
    (doseq [[tag posts] posts-by-tag]
      (let [{:keys [name uri]} (tag-info params tag)]
        (println "-->" (cyan uri))
        (write-html uri
                    params
                    (render-file "/html/tag.html"
                                 (merge params
                                        {:active-page     "tags"
                                         :servlet-context (cryogen-io/path "/" blog-prefix "/")
                                         :name            name
                                         :posts           posts
                                         :uri             uri})))))))

(defn compile-tags-page [{:keys [blog-prefix] :as params}]
  "Compiles a page with links to each tag page. Spits the page into the public folder"
  (println (blue "compiling tags page"))
  (let [uri (page-uri "tags.html" params)]
    (write-html uri
                params
                (render-file "/html/tags.html"
                             (merge params
                                    {:active-page     "tags"
                                     :servlet-context (cryogen-io/path "/" blog-prefix "/")
                                     :uri             uri})))))

(defn content-until-more-marker
  "Returns the content until the <!--more--> special comment,
  closing any unclosed tags. Returns nil if there's no such comment."
  [content]
  (when-let [index (s/index-of content "<!--more-->")]
    (->> (subs content 0 index)
         enlive/html-snippet
         enlive/emit*
         (apply str))))

(defn create-preview
  "Creates a single post preview"
  [blocks-per-preview post]
  (update post :content
          #(or (content-until-more-marker %)
               (->> (enlive/html-snippet %)
                    (take blocks-per-preview)
                    enlive/emit*
                    (apply str)))))

(defn create-previews
  "Returns a sequence of vectors, each containing a set of post previews"
  [posts posts-per-page blocks-per-preview]
  (->> posts
       (map #(create-preview blocks-per-preview %))
       (partition-all posts-per-page)
       (map-indexed (fn [i v] {:index (inc i) :posts v}))))

(defn create-preview-links
  "Turn each vector of previews into a map with :prev and :next keys that contain the uri of the
  prev/next preview page"
  [previews params]
  (mapv (fn [[prev target next]]
          (merge target
                 {:prev (if prev (page-uri (cryogen-io/path "p" (str (:index prev) ".html")) params) nil)
                  :next (if next (page-uri (cryogen-io/path "p" (str (:index next) ".html")) params) nil)}))
        (partition 3 1 (flatten [nil previews nil]))))

(defn compile-preview-pages
  "Compiles a series of pages containing 'previews' from each post"
  [{:keys [blog-prefix posts-per-page blocks-per-preview] :as params} posts]
  (when-not (empty? posts)
    (let [previews (-> posts
                       (create-previews posts-per-page blocks-per-preview)
                       (create-preview-links params))
          previews (if (> (count previews) 1)
                     (assoc-in previews [1 :prev] (page-uri "index.html" params))
                     previews)]
      (cryogen-io/create-folder (cryogen-io/path "/" blog-prefix "p"))
      (doseq [{:keys [index posts prev next]} previews
              :let [index-page? (= 1 index)]]
        (write-html
          (if index-page? (page-uri "index.html" params)
                          (page-uri (cryogen-io/path "p" (str index ".html")) params))
          params
          (render-file "/html/previews.html"
                       (merge params
                              {:active-page     "preview"
                               :home            (when index-page? true)
                               :servlet-context (cryogen-io/path "/" blog-prefix "/")
                               :posts           posts
                               :prev-uri        prev
                               :next-uri        next})))))))

(defn compile-index
  "Compiles the index page into html and spits it out into the public folder"
  [{:keys [disqus? debug? home-page] :as params}]
  (println (blue "compiling index"))
  (let [uri (page-uri "index.html" params)]
    (when debug?
      (print-debug-info meta))
    (write-html uri
                params
                (render-file (str "/html/" (:layout home-page))
                             (merge params
                                    {:active-page "home"
                                     :home        true
                                     :disqus?     disqus?
                                     :uri         uri
                                     :post        home-page
                                     :page        home-page})))))

(defn compile-archives
  "Compiles the archives page into html and spits it out into the public folder"
  [{:keys [blog-prefix] :as params} posts]
  (println (blue "compiling archives"))
  (let [uri (page-uri "archives.html" params)]
    (write-html uri
                params
                (render-file "/html/archives.html"
                             (merge params
                                    {:active-page     "archives"
                                     :archives        true
                                     :groups          (group-for-archive posts)
                                     :servlet-context (cryogen-io/path "/" blog-prefix "/")
                                     :uri             uri})))))

(defn compile-authors
  "For each author, creates a page with filtered posts."
  [{:keys [blog-prefix author-root-uri author] :as params} posts]
  (println (blue "compiling authors"))
  (cryogen-io/create-folder (cryogen-io/path "/" blog-prefix author-root-uri))
  ;; if the post author is empty defaults to config's :author
  (doseq [{:keys [author posts]} (group-for-author posts author)]
    (let [uri (page-uri (str author ".html") :author-root-uri params)]
      (println "-->" (cyan uri))
      (write-html uri
                  params
                  (render-file "/html/author.html"
                               (merge params
                                      {:author          author
                                       :groups          (group-for-archive posts)
                                       :servlet-context (cryogen-io/path "/" blog-prefix "/")
                                       :uri             uri}))))))

(defn tag-posts
  "Converts the tags in each post into links"
  [posts config]
  (map #(update-in % [:tags] (partial map (partial tag-info config))) posts))

(defn- template-dir?
  "Checks that the dir exists in the templates directory."
  [dir]
  (.isDirectory (io/file (str "resources/templates/" dir))))

(defn- markup-entries [post-root page-root]
  (let [entries (for [mu (m/markups)
                      t  (distinct [post-root page-root])]
                  [(str (m/dir mu) "/" t) t])]
    (apply concat entries)))

(defn copy-resources-from-markup-folders
  "Copy resources from markup folders. This does not copy the markup entries."
  [{:keys [post-root page-root] :as config}]
  (let [folders (->> (markup-entries post-root page-root)
                     (filter template-dir?))]
    (cryogen-io/copy-resources
      (merge config
             {:resources     folders
              :ignored-files (map #(re-pattern-from-ext (m/ext %)) (m/markups))}))))

(defn read-config
  "Reads the config file"
  []
  (try
    (let [config (-> "templates/config.edn"
                     cryogen-io/get-resource
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
                     (update-in [:sass-path] (fnil str "sass"))
                     (update-in [:compass-path] (fnil str "compass"))
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
  (let [{:keys [^String site-url blog-prefix rss-name recent-posts sass-dest keep-files ignored-files previews? author-root-uri theme]
         :as   config} (read-config)
        posts        (add-prev-next (read-posts config))
        posts-by-tag (group-by-tags posts)
        posts        (tag-posts posts config)
        latest-posts (->> posts (take recent-posts) vec)
        pages        (read-pages config)
        home-page    (->> pages
                          (filter #(boolean (:home? %)))
                          (first))
        other-pages  (->> pages
                          (remove #{home-page})
                          (add-prev-next))
        [navbar-pages
         sidebar-pages] (group-pages other-pages)
        params       (merge
                       config
                       {:today         (java.util.Date.)
                        :title         (:site-title config)
                        :active-page   "home"
                        :tags          (map (partial tag-info config) (keys posts-by-tag))
                        :latest-posts  latest-posts
                        :navbar-pages  navbar-pages
                        :sidebar-pages sidebar-pages
                        :home-page     (if home-page
                                         home-page
                                         (assoc (first latest-posts) :layout "home.html"))
                        :archives-uri  (page-uri "archives.html" config)
                        :index-uri     (page-uri "index.html" config)
                        :tags-uri      (page-uri "tags.html" config)
                        :rss-uri       (cryogen-io/path "/" blog-prefix rss-name)
                        :site-url      (if (.endsWith site-url "/") (.substring site-url 0 (dec (count site-url))) site-url)})]

    (set-custom-resource-path! (str "file:resources/templates/themes/" theme))
    (cryogen-io/wipe-public-folder keep-files)
    (println (blue "copying theme resources"))
    (cryogen-io/copy-resources-from-theme config)
    (println (blue "copying resources"))
    (cryogen-io/copy-resources config)
    (copy-resources-from-markup-folders config)
    (compile-pages params other-pages)
    (compile-posts params posts)
    (compile-tags params posts-by-tag)
    (compile-tags-page params)
    (if previews?
      (compile-preview-pages params posts)
      (compile-index params))
    (compile-archives params posts)
    (when author-root-uri
      (println (blue "generating authors views"))
      (compile-authors params posts))
    (println (blue "generating site map"))
    (->> (sitemap/generate site-url ignored-files)
         (cryogen-io/create-file (cryogen-io/path "/" blog-prefix "sitemap.xml")))
    (println (blue "generating main rss"))
    (->> (rss/make-channel config posts)
         (cryogen-io/create-file (cryogen-io/path "/" blog-prefix rss-name)))
    (println (blue "generating filtered rss"))
    (rss/make-filtered-channels config posts-by-tag)
    (println (blue "compiling sass"))
    (sass/compile-sass->css!
      (merge (select-keys config [:sass-path :compass-path :sass-src :ignored-files])
             {:dest-sass (cryogen-io/path ".." "public" blog-prefix sass-dest)
              :base-dir  "resources/templates/"}))))

(defn compile-assets-timed []
  (time
    (try
      (compile-assets)
      (catch Exception e
        (if (or (instance? IllegalArgumentException e)
                (instance? clojure.lang.ExceptionInfo e))
          (println (red "Error:") (yellow (.getMessage e)))
          (write-exception e))))))
