(ns cryogen-core.toc-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [net.cgrand.enlive-html :as enlive]
            [cryogen-core.toc :refer :all]))

(defn collapse-lists
  "Given hiccup containing lists, will collapse the lists into
  the surrounding elements (like hiccup does in the process of compiling html)"
  [xs]
  (when xs
    (reduce (fn [acc x]
              (cond (vector? x) (conj acc (collapse-lists x))
                    (sequential? x) (into acc (collapse-lists x))
                    :else (conj acc x)))
            [], xs)))

(deftest collapse-lists-test
  (is (nil? (collapse-lists nil)))
  (is (= [:foo :bar] (collapse-lists [:foo '(:bar)])))
  (is (= [:foo [:bar] [:baz]] (collapse-lists [:foo '([:bar] [:baz])]))))

(defn hic=
  [x & xs]
  (apply = (map collapse-lists (cons x xs))))

; Test that the get-headings function properly filters non-headers
(deftest test-get-headings
  (let [noisy-headers (enlive/html [:div
                                    [:h1 "First H1"]
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
(deftest test-generate-toc*
  (is (hic= [:ol.content [:li [:a {:href "#test"} "Test"]]]
            (-> [:div [:h2 [:a {:name "test"}] "Test"]]
                (enlive/html)
                (generate-toc* :ol))))

  (is (-> [:div [:p "This is not a header"]]
          (enlive/html)
          (generate-toc* :ol)
          (nil?)))

  (is (hic= [:ol.content
             [:li [:a {:href "#starting_low"} "Starting Low"]]
             [:li [:a {:href "#finishing_high"} "Finishing High"]]]
            (-> [:div
                 [:h2 [:a {:name "starting_low"}]
                  "Starting Low"]
                 [:h1 [:a {:name "finishing_high"}]
                  "Finishing High"]]
                (enlive/html)
                (generate-toc* :ol)))
      "No outer header should be less indented than the first header tag.")

  (is (hic= [:ul.content
             [:li [:a {:href "#starting_low"} "Starting Low"]]
             [:ul
              [:li [:a {:href "#jumping_in"} "Jumping Right In"]]
              [:li [:a {:href "#pulling_back"} "But then pull back"]]]
             [:li [:a {:href "#to_the_top"} "To the top"]]]
            (-> [:div
                 [:h2 [:a {:name "starting_low"}]
                  "Starting Low"]
                 [:h4 [:a {:name "jumping_in"}]
                  "Jumping Right In"]
                 [:h3 [:a {:name "pulling_back"}]
                  "But then pull back"]
                 [:h2 [:a {:name "to_the_top"}]
                  "To the top"]]
                (enlive/html)
                (generate-toc* :ul)))
      (str "Inner headers can be more indented, "
           "but outer headers cannot be less indented "
           "than the original header."))

  (is (hic= [:ol.content
             [:li [:a {:href "#foo_<code>bar</code>"} "foo " [:code "bar"]]]]
            (-> [:div [:h2 {:id "foo_<code>bar</code>"} "foo " [:code "bar"]]]
                (enlive/html)
                (generate-toc* :ol)))
      "Supports code tags in headings.")

  (is (hic= [:ol.content
             [:li [:a {:href "#foo_<strong>bar_<i>baz</i></strong>"}
                   "foo " [:strong "bar " [:i "baz"]]]]]
            (-> [:div [:h2 {:id "foo_<strong>bar_<i>baz</i></strong>"}
                       "foo " [:strong "bar " [:i "baz"]]]]
                (enlive/html)
                (generate-toc* :ol)))
      "Supports nested tags in headings."))

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
