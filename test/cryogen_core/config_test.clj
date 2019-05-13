(ns cryogen-core.config-test
  (:require [clojure.test :refer :all]
            [cryogen-core.config :refer :all]))

(def default-config
  {:site-title           "My Awesome Blog"
   :author               "Bob Bobbert"
   :description          "This blog is awesome"
   :site-url             "http://blogawesome.com/"
   :post-root            "posts"
   :page-root            "pages"
   :post-root-uri        "posts-output"
   :page-root-uri        "pages-output"
   :tag-root-uri         "tags-output"
   :author-root-uri      "authors-output"
   :blog-prefix          "/blog"
   :rss-name             "feed.xml"
   :rss-filters          ["cryogen"]
   :recent-posts         3
   :post-date-format     "yyyy-MM-dd"
   :archive-group-format "yyyy MMMM"
   :sass-src             []
   :sass-dest            nil
   :sass-path            "sass"
   :compass-path         "compass"
   :theme                "blue"
   :resources            ["img"]
   :keep-files           [".git"]
   :disqus?              false
   :disqus-shortname     ""
   :ignored-files        [#"\.#.*" #".*\.swp$"]
   :posts-per-page       5
   :blocks-per-preview   2
   :previews?            false
   :clean-urls?          true
   :collapse-subdirs?    false
   :hide-future-posts?   true
   :klipse               {}
   :debug?               false})

(deftest test-config-parsing
  (testing "Parsing configuration file"
    (is (process-config default-config))))

(deftest test-validate-public-dest-path
  (let [with-dest #(assoc default-config :public-dest %)
        resources-in-dest (assoc default-config :resources ["resources/img"] :public-dest "resources")
        dest-in-resources (assoc default-config :resources ["resources"] :public-dest "resources/public")
        ok-config (assoc default-config :resources ["resources/img"] :public-dest "resources/public")
        weird-but-ok-config (assoc default-config :resources ["res"] :public-dest "resources/public")]
    (testing "Throws if path is root or standard folders"
      (is (thrown? Exception (process-config (with-dest ""))))
      (is (thrown? Exception (process-config (with-dest " "))))
      (is (thrown? Exception (process-config (with-dest "."))))
      (is (thrown? Exception (process-config (with-dest "./"))))
      (is (thrown? Exception (process-config (with-dest "content"))))
      (is (thrown? Exception (process-config (with-dest "themes"))))
      (is (thrown? Exception (process-config (with-dest "src"))))
      (is (thrown? Exception (process-config (with-dest "target")))))
    (testing "Throws if dest contains resources"
      (is (thrown? Exception (process-config resources-in-dest))))
    (testing "Throws if resources contains dest"
      (is (thrown? Exception (process-config dest-in-resources))))
    (testing "Is OK if dest is side-by-side with resources"
      (is (process-config ok-config)))
    (testing "Is OK if names are substrings but actually different paths"
      (is (process-config weird-but-ok-config)))))

(deftest test-config-merging
  (let [config {:scalar "orig" :map {:k "orig" :submap {:k "suborig"}} :vec [:orig1 :orig2]}]
    (testing "Merging with overrides"
      (let [override-config (deep-merge true
                                        config
                                        {:scalar "override"
                                         :map {:k "override" :submap {:k "override"}}
                                         :vec ["override"]})]
        (is (= "override" (:scalar override-config)))
        (is (= "override" (get-in override-config [:map :k])))
        (is (= "override" (get-in override-config [:map :submap :k])))
        (is (and (some #{"override"} (:vec override-config))
                 (not-any? #{:orig1} (:vec override-config))))))

    (testing "Merging without overrides"
      (let [override-config (deep-merge false
                                        config
                                        {:scalar "override"
                                         :map {:k "override" :submap {:k "override"}}
                                         :vec ["added"]})]
        (is (= "override" (:scalar override-config)))
        (is (= "override" (get-in override-config [:map :k])))
        (is (= "override" (get-in override-config [:map :submap :k])))
        (is (every? #{"added" :orig1 :orig2} (:vec override-config)))))))