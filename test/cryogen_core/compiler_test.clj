(ns cryogen-core.compiler-test
  (:require [clojure.test :refer :all]
            [cryogen-core.compiler :refer :all]
            [cryogen-core.markup :as m]
            [me.raynes.fs :as fs])
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
         "<div id=\"post\"><div class=\"post-content\">
    this post has more marker
</div></div>")))

(defn- markdown []
  (reify m/Markup
    (dir [this] "md")
    (ext [this] ".md")))

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
