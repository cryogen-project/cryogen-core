(ns cryogen-core.compiler-test
  (:require [clojure.test :refer :all]
            [me.raynes.fs :as fs]
            [cryogen-core.compiler :refer :all]
            [cryogen-core.markup :as m])
  (:import [java.io File]))

; Test that the content-until-more-marker return nil or correct html text.
(deftest test-content-until-more-marker
  ; text without more marker, return nil
  (is (nil? (content-until-more-marker "<div id=\"post\">
  <div class=\"post-content\">
    this post does not have more marker
  </div>
</div>")))
  ; text with more marker, return text before more marker with closing tags.
  (is (= (content-until-more-marker "<div id='post'>
  <div class='post-content'>
    this post has more marker
<!--more-->
and more content.
  </div>
</div>")
         "<div id=\"post\">
  <div class=\"post-content\">
    this post has more marker
</div></div>")))

(defn- markdown []
  (reify m/Markup
    (dir [this] "md")
    (ext [this] ".md")))

(defn- asciidoc []
  (reify m/Markup
    (dir [this] "asc")
    (ext [this] ".asc")))

(defn- create-entry [dir file]
  (fs/mkdirs (File. dir))
  (fs/create (File. (str dir File/separator file))))

(defn- reset-resources []
  (fs/delete-dir "resources")
  (create-entry "resources" ".gitkeep"))

(defn- check-for-pages [mu]
  (find-pages {:page-root "pages"} mu))

(defn- check-for-posts [mu]
  (find-posts {:post-root "posts"} mu))

(deftest test-find-entries
  (reset-resources)
  (let [mu (markdown)]
    (testing "Finds no files"
      (is (empty? (check-for-posts mu))
      (is (empty? (check-for-pages mu))))

    (let [dir->file
          [[check-for-posts "resources/templates/md/posts" "post.md"]
           [check-for-posts "resources/templates/posts" "post.md"]
           [check-for-pages "resources/templates/md/pages" "page.md"]
           [check-for-pages "resources/templates/pages" "page.md"]]]
      (doseq [[check-fn dir file] dir->file]
        (testing (str "Finds files in " dir)
          (create-entry dir file)
          (let [entries (check-fn mu)]
            (is (= 1 (count entries)))
            (is (= (.getAbsolutePath (File. (str dir File/separator file)))
                   (.getAbsolutePath (first entries)))))
          (reset-resources)))))))

(defmacro with-markup [mu & body]
  `(do
     (m/register-markup ~mu)
     (try
       ~@body
       (finally
         (m/clear-registry)))))

(defn- copy-and-check-markup-folders
  "Create entries in the markup folders. If `with-dir?` is set to true, include
  the Markup implementation's `dir` in the path. Check that the folders exist
  in the output folder."
  [[pages-root posts-root :as dirs] mu with-dir?]
  (doseq [dir dirs]
    (let [path (if with-dir?
                 (str (m/dir mu) "/" dir)
                 dir)]
      (create-entry (str "resources/templates/" path)
                    (str "entry" (m/ext mu)))))
  (with-markup mu
    (copy-resources-from-markup-folders
      {:post-root posts-root
       :page-root pages-root
       :blog-prefix "/blog"}))
  (doseq [dir dirs]
    (is (.isDirectory (File. (str "resources/public/blog/" dir))))))

(deftest test-copy-resources-from-markup-folders
  (reset-resources)
  (testing "No pages or posts nothing to copy"
    (copy-resources-from-markup-folders
      {:post-root "pages"
       :page-root "posts"
       :blog-prefix "/blog"})
    (is (not (.isDirectory (File. (str "resources/public/blog/pages")))))
    (is (not (.isDirectory (File. (str "resources/public/blog/posts"))))))

  (reset-resources)
  (doseq [mu [(markdown) (asciidoc)]]
    (testing (str "Test copy from markup folders (" (m/dir mu) ")")
      (let [dirs ["pages" "posts"]]
        (copy-and-check-markup-folders dirs mu true)
        (reset-resources)
        (copy-and-check-markup-folders dirs mu false))))
  (reset-resources))


(defn- page [uri page-index] 
  {:navmap? true, :uri uri :content uri :page-index page-index})

(defn- enhanced-page [uri page-index children] 
  {:navmap? true, :uri uri :content uri
   :page-index page-index
   :navmap-children children})
           
(deftest test-navmap-pages
  (testing 
    "No pages or posts nothing to copy"
    (let [pages [{:navmap? false, :content "1"} 
                 (page "/pages/nav1/" 0) 
                 (page "/pages/nav1/nav11/" 1)
                 (page "/pages/nav1/nav13/" 3)
                 (page "/pages/nav1/nav11/nav112/" 2)
                 (page "/pages/nav1/nav12/" 2)
                 (page "/pages/nav1/nav11/xnav111/" 1)
                 ]
          expected [(enhanced-page 
                      "/pages/nav1/" 0
                      [(enhanced-page 
                         "/pages/nav1/nav11/" 1
                         [(page "/pages/nav1/nav11/xnav111/" 1)
                          (page "/pages/nav1/nav11/nav112/" 2)])
                       (page "/pages/nav1/nav12/" 2)
                       (page "/pages/nav1/nav13/" 3)]
                      )] 
          ]        
      (is (= expected
             (build-nav-map pages)))
      )
    ))
