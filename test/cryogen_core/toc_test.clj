(ns cryogen-core.toc-test
  (:require [clojure.test :refer :all]
            [clojure.string :refer [join]]
            [clojure.zip :as z]
            [crouton.html :as html]
            [hiccup.core :as hiccup]
            [cryogen-core.toc :refer :all]))

; Reimport private functions
(def get-headings #'cryogen-core.toc/get-headings)
(def make-toc-entry #'cryogen-core.toc/make-toc-entry)
(def zip-toc-tree-to-insertion-point #'cryogen-core.toc/zip-toc-tree-to-insertion-point)
(def insert-toc-tree-entry #'cryogen-core.toc/insert-toc-tree-entry)
(def build-toc-tree #'cryogen-core.toc/build-toc-tree)
(def build-toc #'cryogen-core.toc/build-toc)

(defn parse-to-headings
  [hiccup-seq]
  (-> hiccup-seq hiccup/html html/parse-string :content get-headings))

; Test that the get-headings function properly filters non-headers
(deftest test-get-headings
  (let [noisy-headers [:div [:h1 "First H1"]
                            [:p "Ignore..."]
                            [:h2 "First H2"]]]
    (is (= (parse-to-headings noisy-headers)
         [{:tag :h1 :attrs nil :content ["First H1"]}
          {:tag :h2 :attrs nil :content ["First H2"]}]))))

; Test that the make-toc-entry ignores invalid input
(deftest test-make-toc-entry
  (is (nil?
         (make-toc-entry nil "Text")))
  (is (nil?
         (make-toc-entry "anchor" nil)))
  (is (= [:li [:a {:href "#anchor"} "Text"]]
         (make-toc-entry "anchor" "Text"))))

; Test that the built table of contents always treats later
; headers as being at the same level as earlier headers, even
; if the later headers are strictly greater in value.
; E.G.
; * h2
;   * h3
; * h1
(deftest test-build-toc
  (let [simplest-header [:div [:h2 [:a {:name "test"}] "Test"]]
        no-headers [:div [:p "This is not a header"]]
        closing-header-larger-than-opening-1
        [:div [:h2 [:a {:name "starting_low"}]
                    "Starting Low"]
              [:h1 [:a {:name "finishing_high"}]
                    "Finishing High"]]
        closing-header-larger-than-opening-2
        [:div [:h2 [:a {:name "starting_low"}]
                    "Starting Low"]
              [:h4 [:a {:name "jumping_in"}]
                    "Jumping Right In"]
              [:h3 [:a {:name "pulling_back"}]
                    "But then pull back"]
              [:h2 [:a {:name "to_the_top"}]
                    "To the top"]]]
    (is (= [:ol.content (seq [[:li [:a {:href "#test"} "Test"]]])]
           (-> simplest-header parse-to-headings build-toc-tree
               (build-toc :ol))))
    (is (nil?
        (-> no-headers parse-to-headings build-toc-tree
            (build-toc :ol))))
    (is (= [:ol.content (seq [[:li [:a {:href "#starting_low"} "Starting Low"]]
                              [:li [:a {:href "#finishing_high"} "Finishing High"]]])]
         (-> closing-header-larger-than-opening-1
             parse-to-headings 
             build-toc-tree 
             (build-toc :ol)))
        "No outer header should be less indented than the first header tag.")
    (is (= [:ul.content 
            (seq [
                (seq [
                  [:li [:a {:href "#starting_low"} "Starting Low"]]
                  [:ul 
                   (seq [
                         [:li [:a {:href "#jumping_in"} "Jumping Right In"]]
                         [:li [:a {:href "#pulling_back"} "But then pull back"]]
                         ])
                   ] ])
                  [:li [:a {:href "#to_the_top"} "To the top"]]
                  ])]
           (-> closing-header-larger-than-opening-2
               parse-to-headings
               build-toc-tree
               (build-toc :ul)))
        (join "" ["Inner headers can be more indented, "
                  "but outer headers cannot be less indented "
                  "than the original header."]))
    ))
