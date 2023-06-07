(ns cryogen-core.infer-meta-test
  (:require [clojure.java.io :refer [file]]
            [clojure.test :refer :all]
            [cryogen-core.infer-meta :refer [infer-image-data infer-title]]))

(deftest infer-title-test
  (testing "infer-title from H1"
    (let [dom (list {:tag :h1, :attrs {:id "this-is-a-test-h1"}, :content (list "This is a test H1")}
               {:tag :p,
                :attrs nil,
                :content (list "Testing new post metadata inference.")})
          page (file "../content/md/posts/2023-06-06-this-is-a-test-filename.md")
          config {:post-date-format "yyyy-mm-dd"}
          expected "This is a test H1"
          actual (infer-title page config dom)]
      (is (= actual expected))))
  (testing "infer-title from filename"
    (let [dom (list {:tag :h2, :attrs {:id "this-is-a-test-h2"}, :content (list "This is a test H2")}
                    {:tag :p,
                     :attrs nil,
                     :content (list "Testing new post metadata inference.")})
          page (file "../content/md/posts/2023-06-06-this-is-a-test-filename.md")
          config {:post-date-format "yyyy-mm-dd"}
          expected "This is a test filename"
          actual (infer-title page config dom)]
      (is (= actual expected)))))

(deftest infer-image-data-test
  (let [dom (list {:tag :h1, :attrs {:id "this-is-a-test"}, :content (list "This is a test")}
             {:tag :p,
              :attrs nil,
              :content
              (list {:tag :img,
                :attrs
                {:src "/blog/img/uploads/refugeeswelcome.png",
                 :alt "This is an image"},
                :content nil})}
             {:tag :p,
              :attrs nil,
              :content (list "Testing new post metadata inference.")})
        expected "/img/uploads/refugeeswelcome.png"
        actual (infer-image-data dom {:blog-prefix "/blog"})]
    (is (= actual expected))))