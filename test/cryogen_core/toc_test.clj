(ns cryogen-core.toc-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [net.cgrand.enlive-html :as enlive]
            [cryogen-core.toc :refer :all]))

; Reimport private functions
(def build-toc-tree #'cryogen-core.toc/build-toc-tree)
(def build-toc #'cryogen-core.toc/build-toc)

; Test that the get-headings function properly filters non-headers
(deftest test-get-headings
  (let [noisy-headers (enlive/html [:div [:h1 "First H1"]
                                    [:p "Ignore..."]
                                    [:h2 "First H2"]])]
    (is (= (get-headings noisy-headers)
           [{:tag :h1 :attrs {} :content ["First H1"]}
            {:tag :h2 :attrs {} :content ["First H2"]}]))))

; Test that the toc-entry ignores invalid input
(deftest test-toc-entry
  (is (nil?
       (toc-entry nil "Text")))
  (is (nil?
       (toc-entry "anchor" nil)))
  (is (= [:li [:a {:href "#anchor"} "Text"]]
         (toc-entry "anchor" "Text"))))

; Test that the built table of contents always treats later
; headers as being at the same level as earlier headers, even
; if the later headers are strictly greater in value.
; E.G.
; * h2
;   * h3
; * h1
(deftest test-build-toc
  (let [simplest-header [:div [:h2 [:a {:name "test"}] "Test"]]
        no-headers      [:div [:p "This is not a header"]]

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
    (is (= [:ol.content [[:li [:a {:href "#test"} "Test"]]]]
           (-> simplest-header
               (enlive/html)
               (get-headings)
               (build-toc-tree)
               (build-toc :ol))))
    (is (-> no-headers
            (enlive/html)
            (get-headings)
            (build-toc-tree)
            (build-toc :ol)
            (nil?)))
    (is (= [:ol.content [[:li [:a {:href "#starting_low"} "Starting Low"]]
                         [:li [:a {:href "#finishing_high"} "Finishing High"]]]]
           (-> closing-header-larger-than-opening-1
               (enlive/html)
               (get-headings)
               (build-toc-tree)
               (build-toc :ol)))
        "No outer header should be less indented than the first header tag.")
    (is (= [:ul.content
            [[[:li [:a {:href "#starting_low"} "Starting Low"]]
              [:ul
               [[:li [:a {:href "#jumping_in"} "Jumping Right In"]]
                [:li [:a {:href "#pulling_back"} "But then pull back"]]]]]
             [:li [:a {:href "#to_the_top"} "To the top"]]]]
           (-> closing-header-larger-than-opening-2
               (enlive/html)
               (get-headings)
               (build-toc-tree)
               (build-toc :ul)))
        (str "Inner headers can be more indented, "
             "but outer headers cannot be less indented "
             "than the original header."))))


(deftest test-generate-toc
  (let [htmlString "<div><h2><a name=\"test\"></a>Test</h2></div>"]
    (is (= "<ol class=\"content\"><li><a href=\"#test\">Test</a></li></ol>"
           (generate-toc htmlString)))
    (is (= "<ol class=\"content\"><li><a href=\"#test\">Test</a></li></ol>"
           (generate-toc htmlString :list-type true)))
    (is (= "<ol class=\"content\"><li><a href=\"#test\">Test</a></li></ol>"
           (generate-toc htmlString :list-type :ol)))
    (is (= "<ul class=\"content\"><li><a href=\"#test\">Test</a></li></ul>"
           (generate-toc htmlString :list-type :ul)))))
