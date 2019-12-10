(ns cryogen-core.toc-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [net.cgrand.enlive-html :as enlive]
            [cryogen-core.util :as util]
            [cryogen-core.toc :refer :all]))

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
  (is (util/hic= [:ol.toc [:li [:a {:href "#test"} "Test"]]]
                 (-> [:div [:h2 {:id "test"} "Test"]]
                     (enlive/html)
                     (generate-toc* :ol "toc"))))

  (is (-> [:div [:p "This is not a header"]]
          (enlive/html)
          (generate-toc* :ol "toc")
          (nil?)))

  (is (util/hic= [:ol.toc
                  [:li [:a {:href "#starting_low"} "Starting Low"]]
                  [:li [:a {:href "#finishing_high"} "Finishing High"]]]
                 (-> [:div
                      [:h2 {:id "starting_low"}
                       "Starting Low"]
                      [:h1 {:id "finishing_high"}
                       "Finishing High"]]
                     (enlive/html)
                     (generate-toc* :ol "toc")))
      "No outer header should be less indented than the first header tag.")

  (is (util/hic= [:ul.toc
                  [:li [:a {:href "#starting_low"} "Starting Low"]]
                  [:ul
                   [:li [:a {:href "#jumping_in"} "Jumping Right In"]]
                   [:li [:a {:href "#pulling_back"} "But then pull back"]]]
                  [:li [:a {:href "#to_the_top"} "To the top"]]]
                 (-> [:div
                      [:h2 {:id "starting_low"}
                       "Starting Low"]
                      [:h4 {:id "jumping_in"}
                       "Jumping Right In"]
                      [:h3 {:id "pulling_back"}
                       "But then pull back"]
                      [:h2 {:id "to_the_top"}
                       "To the top"]]
                     (enlive/html)
                     (generate-toc* :ul "toc")))
      (str "Inner headers can be more indented, "
           "but outer headers cannot be less indented "
           "than the original header."))

  (is (util/hic= [:ol.toc
                  [:li [:a {:href "#foo_<code>bar</code>"} "foo " [:code "bar"]]]]
                 (-> [:div [:h2 {:id "foo_<code>bar</code>"} "foo " [:code "bar"]]]
                     (enlive/html)
                     (generate-toc* :ol "toc")))
      "Supports code tags in headings.")

  (is (util/hic= [:ol.toc
                  [:li [:a {:href "#foo_<strong>bar_<i>baz</i></strong>"}
                        "foo " [:strong "bar " [:i "baz"]]]]]
                 (-> [:div [:h2 {:id "foo_<strong>bar_<i>baz</i></strong>"}
                            "foo " [:strong "bar " [:i "baz"]]]]
                     (enlive/html)
                     (generate-toc* :ol "toc")))
      "Supports nested tags in headings."))

(deftest test-generate-toc
  (let [htmlString "<div><h2 id=\"test\">Test</h2></div>"]
    (is (= "<ol class=\"toc\"><li><a href=\"#test\">Test</a></li></ol>"
           (generate-toc htmlString {:list-type true :toc-class "toc"})))
    (is (= "<ol class=\"toc\"><li><a href=\"#test\">Test</a></li></ol>"
           (generate-toc htmlString {:list-type :ol :toc-class "toc"})))
    (is (= "<ul class=\"custom-toc\"><li><a href=\"#test\">Test</a></li></ul>"
           (generate-toc htmlString {:list-type :ul :toc-class "custom-toc"})))))
