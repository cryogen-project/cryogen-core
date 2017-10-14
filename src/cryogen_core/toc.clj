(ns cryogen-core.toc
  (:require [clojure.zip :as z]
            [net.cgrand.enlive-html :as enlive]
            [hiccup.core :as hiccup]))

(def _h [:h1 :h2 :h3 :h4 :h5 :h6])

(defn- compare-index [i1 i2] (- (.indexOf ^clojure.lang.APersistentVector _h i2) (.indexOf ^clojure.lang.APersistentVector _h i1)))

(defn- get-headings
  "Turn a body of html content into a vector of elements whose tags are
  headings."
  [content]
  (reduce
    (fn [headings {:keys [tag attrs content] :as elm}]
      (if (some #{tag} _h)
        (conj headings elm)
        (if-let [more-headings (get-headings content)]
          (into headings more-headings)
          headings)))
    [] content))

(defn- zip-toc-tree-to-insertion-point
  "Given a toc-tree zipper and a header level, navigate
  the zipper to the appropriate parent of the level for that header
  to be inserted and return the zipper."
  [toctree h-tag]
  (if-let [current-tag (-> toctree first :value :tag)]
    (let [direction (compare-index h-tag current-tag)]
      (cond (zero? direction) (z/up toctree)                  ; Tag belongs at current level
            (neg? direction) toctree                         ; Tag belongs below this level
            (pos? direction) (recur (z/up toctree) h-tag)))  ; Keep looking up
    ; This level is the root list, return it
    toctree))

(defn- insert-toc-tree-entry
  "Inserts a toc-tree (zipper) entry for the given entry at the appropriate place.
  Obeys the invariant that the toc-tree (zipper) is always moved to the inserted loc."
  [tree entry]
  (let [{htag :tag} entry
        tree (zip-toc-tree-to-insertion-point tree htag)]
    (-> tree (z/append-child {:children [] :value entry}) z/down z/rightmost)))

(defn- build-toc-tree
  "Given a sequence of header nodes, build a toc tree using zippers
  and return it."
  [headings]
  (loop [zp (z/zipper
              map?
              :children
              (fn [node children] (assoc node :children (apply vector children)))
              {:value :root :children []})
         items headings]
    ;; I pulled the giant gnarly destructuring out into multiple ones
    ;; so I could grok it better. It should be equivalent.
    (if-let [item (first items)]
      (let [{:keys [tag attrs content]} item
            {:keys [id]} attrs
            [{{:keys [name]} :attrs} title :as htext] content
            anchor (or id name)]
        (if (nil? anchor)
          (recur zp (rest items))
          (recur (insert-toc-tree-entry
                  zp
                  {:tag tag
                   :anchor anchor

                   ;; This fixes the issue, but a bunch of tests fail.
                   ;; Would be nice with some input about what's supposed to happen here.
                   :text (apply str (enlive/emit* content))
                   #_(or
                      (if (string? title) title (-> title :content first))
                      (first htext))})
                 (rest items))))
      (z/root zp))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; The test failures

;; FAIL in (test-build-toc) (toc_test.clj:62)
;; expected: (= [:ol.content (seq [[:li [:a {:href "#test"} "Test"]]])] (-> simplest-header (parse-to-headings) (build-toc-tree) (build-toc :ol)))
;; actual: (not (= [:ol.content ([:li [:a {:href "#test"} "Test"]])] [:ol.content ([:li [:a {:href "#test"} "<a name=\"test\"></a>Test"]])]))

;; lein test :only cryogen-core.toc-test/test-build-toc

;; FAIL in (test-build-toc) (toc_test.clj:72)
;; No outer header should be less indented than the first header tag.
;; expected: (= [:ol.content (seq [[:li [:a {:href "#starting_low"} "Starting Low"]] [:li [:a {:href "#finishing_high"} "Finishing High"]]])] (-> closing-header-larger-than-opening-1 (parse-to-headings) (build-toc-tree) (build-toc :ol)))
;; actual: (not (= [:ol.content ([:li [:a {:href "#starting_low"} "Starting Low"]] [:li [:a {:href "#finishing_high"} "Finishing High"]])] [:ol.content ([:li [:a {:href "#starting_low"} "<a name=\"starting_low\"></a>Starting Low"]] [:li [:a {:href "#finishing_high"} "<a name=\"finishing_high\"></a>Finishing High"]])]))

;; lein test :only cryogen-core.toc-test/test-build-toc


;; ;; htmlString "<div><h2><a name=\"test\"></a>Test</h2></div>"

;; FAIL in (test-build-toc) (toc_test.clj:79)
;; Inner headers can be more indented, but outer headers cannot be less indented than the original header.
;; expected: (= [:ul.content (seq [(seq [[:li [:a {:href "#starting_low"} "Starting Low"]] [:ul (seq [[:li [:a {:href "#jumping_in"} "Jumping Right In"]] [:li [:a {:href "#pulling_back"} "But then pull back"]]])]]) [:li [:a {:href "#to_the_top"} "To the top"]]])] (-> closing-header-larger-than-opening-2 (parse-to-headings) (build-toc-tree) (build-toc :ul)))
;; actual: (not (= [:ul.content (([:li [:a {:href "#starting_low"} "Starting Low"]] [:ul ([:li [:a {:href "#jumping_in"} "Jumping Right In"]] [:li [:a {:href "#pulling_back"} "But then pull back"]])]) [:li [:a {:href "#to_the_top"} "To the top"]])] [:ul.content (([:li [:a {:href "#starting_low"} "<a name=\"starting_low\"></a>Starting Low"]] [:ul ([:li [:a {:href "#jumping_in"} "<a name=\"jumping_in\"></a>Jumping Right In"]] [:li [:a {:href "#pulling_back"} "<a name=\"pulling_back\"></a>But then pull back"]])]) [:li [:a {:href "#to_the_top"} "<a name=\"to_the_top\"></a>To the top"]])]))

;; lein test :only cryogen-core.toc-test/test-generate-toc

;; FAIL in (test-generate-toc) (toc_test.clj:96)
;; expected: (= "<ol class=\"content\"><li><a href=\"#test\">Test</a></li></ol>"
;;              (generate-toc htmlString))
;; actual: (not (= "<ol class=\"content\"><li><a href=\"#test\">Test</a></li></ol>"
;;                 "<ol class=\"content\"><li><a href=\"#test\"><a name=\"test\"></a>Test</a></li></ol>"))

;; lein test :only cryogen-core.toc-test/test-generate-toc

;; FAIL in (test-generate-toc) (toc_test.clj:98)
;; expected: (= "<ol class=\"content\"><li><a href=\"#test\">Test</a></li></ol>"
;;              (generate-toc htmlString :list-type true))
;; actual: (not (= "<ol class=\"content\"><li><a href=\"#test\">Test</a></li></ol>"
;;                 "<ol class=\"content\"><li><a href=\"#test\"><a name=\"test\"></a>Test</a></li></ol>"))

;; lein test :only cryogen-core.toc-test/test-generate-toc

;; FAIL in (test-generate-toc) (toc_test.clj:100)
;; expected: (= "<ol class=\"content\"><li><a href=\"#test\">Test</a></li></ol>"
;;              (generate-toc htmlString :list-type :ol))
;; actual: (not (= "<ol class=\"content\"><li><a href=\"#test\">Test</a></li></ol>"
;;                 "<ol class=\"content\"><li><a href=\"#test\"><a name=\"test\"></a>Test</a></li></ol>"))

;; lein test :only cryogen-core.toc-test/test-generate-toc

;; FAIL in (test-generate-toc) (toc_test.clj:102)
;; expected: (= "<ul class=\"content\"><li><a href=\"#test\">Test</a></li></ul>"
;;              (generate-toc htmlString :list-type :ul))
;; actual: (not (= "<ul class=\"content\"><li><a href=\"#test\">Test</a></li></ul>"
;;                 "<ul class=\"content\"><li><a href=\"#test\"><a name=\"test\"></a>Test</a></li></ul>"))

(defn- make-toc-entry
  "Given an anchor link and some text, construct a toc entry
  consisting of link to the anchor using the given text, wrapped
  in an <li> tag."
  [anchor text]
  (when (and anchor text)
    [:li [:a {:href (str "#" anchor)} text]]))


(defn- build-toc
  "Given the root of a toc tree and either :ol or :ul,
  generate the table of contents and return it as a hiccup tree."
  [toc-tree list-open & {:keys [outer-list?] :or {outer-list? true}}]
  (let [{:keys [children], {:keys [anchor text]} :value} toc-tree
        li (make-toc-entry anchor text)
        first-list-open (if outer-list?
                          (keyword (str (name list-open) ".content"))
                          list-open)]
    ; Create hiccup sequence of :ol/:ul tag and sequence of :li tags
    (if (seq children)
      (let [sublist [first-list-open (map build-toc children
                                       (repeat list-open)
                                       (repeat :outer-list?)
                                       (repeat false))]]
        (if-let [li li] ; The root element has nothing so ignore it
          ;; FIXME: Not really related to anything, but seq does not
          ;;        lazily concat things, what is intended here?
          (seq [li sublist]) ; Use seq to lazily concat the li with the sublists
          sublist))
      li))) ; Or just return the naked :li tag

(defn generate-toc
  "Reads an HTML string and parses it for headers, then returns a list of links
  to them.

  Optionally, a map of :list-type can be provided with value :ul, :ol, or true.
  :ol and true will result in an ordered list being generated for the table of
  contents, while :ul will result in an unordered list. The default is an
  ordered list."
  [html & {:keys [list-type] :or {list-type :ol}}]
  (let [list-type (if (true? list-type) :ol list-type)]
    (-> html
    (enlive/html-snippet)
    (get-headings)
    (build-toc-tree)
    (build-toc list-type)
    (hiccup/html))))
