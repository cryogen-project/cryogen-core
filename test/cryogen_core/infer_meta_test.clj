(ns cryogen-core.infer-meta-test
  (:require [clojure.java.io :refer [file]]
            [clojure.test :refer :all]
            [cryogen-core.infer-meta :refer [clean infer-image-data infer-title main-title main-title?]]))

(deftest infer-title-test
  (testing "infer-title from H1"
    (let [dom '({:tag :h1, :attrs {:id "this-is-a-test-h1"}, :content ("This is a test H1")}
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

;; Not wanting to pollute the repository by adding an image just for testing.
;; (deftest infer-image-data-test
;;   (let [dom (list {:tag :h1, :attrs {:id "this-is-a-test"}, :content (list "This is a test")}
;;              {:tag :p,
;;               :attrs nil,
;;               :content
;;               (list {:tag :img,
;;                 :attrs
;;                 {:src "/blog/img/uploads/refugeeswelcome.png",
;;                  :alt "This is an image"},
;;                 :content nil})}
;;              {:tag :p,
;;               :attrs nil,
;;               :content (list "Testing new post metadata inference.")})
;;         expected "/img/uploads/refugeeswelcome.png"
;;         actual (infer-image-data dom {:blog-prefix "/blog"})]
;;     (is (= actual expected))))

(deftest main-title-test
  (let [dom '({:tag :h1, :attrs {:id "this-is-a-test-h1"}, :content ("This is a test H1")}
              {:tag :p,
               :attrs nil,
               :content (list "Testing new post metadata inference.")}
              {:tag :p,
               :attrs nil,
               :content
               ({:tag :strong, :attrs nil, :content ("Tags:")}
                " Climate, Living Spaces")}
              {:tag :p,
               :attrs nil,
               :content ({:tag :strong, :attrs nil, :content ("Tags:")} " Ecocide")})]
    (let [expected :h1
          actual (:tag (main-title dom))]
      (is (= actual expected) "The main title should be `This is a test H1`."))
    (let [expected true
          actual (main-title? (first dom) dom)]
      (is (= actual expected) "The main title should be the main title."))))

(deftest clean-test
  (let [original '({:tag :h1, :attrs {:id "this-is-a-test-h1"}, :content ("This is a test H1")}
                   {:tag :p,
                    :attrs nil,
                    :content (list "Testing new post metadata inference.")}
                   {:tag :p,
                    :attrs nil,
                    :content
                    ({:tag :strong, :attrs nil, :content ("Tags:")}
                     " Climate, Living Spaces")}
                   {:tag :p,
                    :attrs nil,
                    :content ({:tag :strong, :attrs nil, :content ("Tags:")} " Ecocide")})
        expected '({:tag :p,
                    :attrs nil,
                    :content (list "Testing new post metadata inference.")})
        actual (clean original)]
    (is (= actual expected))))