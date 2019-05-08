(ns cryogen-core.compiler-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
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
  (doseq [dir ["public" "content"]]
    (fs/delete-dir dir)
    (create-entry dir ".gitkeep")))

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
          [[check-for-posts "content/md/posts" "post.md"]
           [check-for-posts "content/posts" "post.md"]
           [check-for-pages "content/md/pages" "page.md"]
           [check-for-pages "content/pages" "page.md"]]]
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
      (create-entry (str "content/" path)
                    (str "entry" (m/ext mu)))))
  (with-markup mu
    (copy-resources-from-markup-folders
      {:post-root posts-root
       :page-root pages-root
       :blog-prefix "/blog"}))
  (doseq [dir dirs]
    (is (.isDirectory (File. (str "public/blog/" dir))))))

(deftest test-copy-resources-from-markup-folders
  (reset-resources)
  (testing "No pages or posts nothing to copy"
    (copy-resources-from-markup-folders
      {:post-root "pages"
       :page-root "posts"
       :blog-prefix "/blog"})
    (is (not (.isDirectory (File. (str "public/blog/pages")))))
    (is (not (.isDirectory (File. (str "public/blog/posts"))))))

  (reset-resources)
  (doseq [mu [(markdown) (asciidoc)]]
    (testing (str "Test copy from markup folders (" (m/dir mu) ")")
      (let [dirs ["pages" "posts"]]
        (copy-and-check-markup-folders dirs mu true)
        (reset-resources)
        (copy-and-check-markup-folders dirs mu false))))
  (reset-resources))

(deftest fail-test (testing "failure" (is true)))

(defn reader-string [s]
  (java.io.PushbackReader. (java.io.StringReader. s)))

(deftest test-metadata-parsing
  (testing "Parsing page/post configuration"
    (let [valid-metadata (reader-string "{:layout :post :title \"Hello World\"}")
          invalid-metadata (reader-string "{:layout \"post\" :title \"Hello World\"}")]
      (is (read-page-meta nil valid-metadata))
      (is (thrown? Exception (read-page-meta nil invalid-metadata))))))
