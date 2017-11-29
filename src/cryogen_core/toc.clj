(ns cryogen-core.toc
  (:require [clojure.zip :as z]
            [cryogen-core.util :as util]
            [net.cgrand.enlive-html :as enlive]
            [hiccup.core :as hiccup]))

(def headings [:h1 :h2 :h3 :h4 :h5 :h6])

(def ^{:arglists '([heading])} heading-index
  (zipmap headings (range)))

(defn get-headings
  "Get all the headings in a sequence of enlive elements."
  [content]
  (util/filter-html-elems (comp (set headings) :tag) content))

(defn compare-index [h1 h2]
  (- (heading-index h2) (heading-index h1)))

(defn toc-entry
  "Given an anchor link and some text, construct a toc entry
  consisting of link to the anchor using the given text, wrapped
  in an <li> tag."
  [anchor text]
  (when (and anchor text)
    [:li [:a {:href (str "#" anchor)} text]]))

(defn- zip-toc-tree-to-insertion-point
  "Given a toc-tree zipper and a header level, navigate
  the zipper to the appropriate parent of the level for that header
  to be inserted and return the zipper."
  [toctree h-tag]
  (if-let [current-tag (-> toctree first :value :tag)]
    (let [direction (compare-index h-tag current-tag)]
      (cond (zero? direction) (z/up toctree)                 ; Tag belongs at current level
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
  (transduce
   (filter (comp :id :attrs))           ; Only include headers that have an id
   (fn
     ([zp] (z/root zp))                 ; Return the root
     ([zp {:keys [tag attrs content]}]
      (insert-toc-tree-entry zp
                             {:tag tag
                              :anchor (:id attrs)
                              :text (util/enlive->hiccup content)})))
   (z/zipper
    map?
    :children
    (fn [node children] (assoc node :children (apply vector children)))
    {:value :root :children []})
   headings))

(defn- build-toc
  "Given the root of a toc tree and either :ol or :ul,
  generate the table of contents and return it as a hiccup tree."
  [toc-tree list-open & {:keys [outer-list?] :or {outer-list? true}}]
  (let [{:keys [children], {:keys [anchor text]} :value} toc-tree
        li (toc-entry anchor text)]
    (if (seq children)
      ;; Create hiccup sequence of :ol/:ul tag and sequence of :li tags
      (list li [list-open
                (when outer-list?
                  {:class "content"})
                (map #(build-toc % list-open :outer-list? false) children)])
      li))) ; Or just return the naked :li tag

(defn generate-toc*
  "The inner part of generate-toc. Takes maps of enlive-style html elements
  and returns hiccup."
  [elements list-type]
  (-> elements
      (get-headings)
      (build-toc-tree)
      (build-toc list-type)))

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
        (generate-toc* list-type)
        (hiccup/html))))
