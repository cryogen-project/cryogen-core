(ns cryogen-core.toc
  (:require [crouton.html :as html]
            [hiccup.core :as hiccup]))

(def _h [:h1 :h2 :h3 :h4 :h5 :h6])
(defn- compare_index [i1 i2] (- (.indexOf ^clojure.lang.APersistentVector _h i2) (.indexOf ^clojure.lang.APersistentVector _h i1)))

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


(defn make-links
  "Create a table of contents from the given headings. This function will look
  for either:
  (1) headings with a child anchor with a non-nil name attribute, e.g.
      <h1><a name=\"reference\">Reference Title</a></h1>
  or
  (2) headings with an id attribute, e.g. <h1 id=\"reference\">Reference Title</h1>
  In both cases above, the anchor reference becomes \"#reference\" and the
  anchor text is \"Reference Title\"."
  [headings li-tags]
  (let [[li-open li-close] li-tags]
    (loop [items headings acc nil _last nil]
    (if-let [{tag :tag {id :id} :attrs [{{name :name} :attrs} title :as htext] :content} (first items)]
      (let [anchor (or id name)]
        (if (nil? anchor)
          (recur (rest items) acc nil)
          (let [entry [:li [:a {:href (str "#" anchor)} (or title (first htext))]]
                jump (compare_index _last tag)]
            (cond (> jump 0) (recur (rest items) (str acc li-open (hiccup/html entry)) tag)
                  (= jump 0) (recur (rest items) (str acc (hiccup/html entry)) tag)
                  (< jump 0) (recur (rest items) (str acc (apply str (repeat (* -1 jump) li-close))
                                                      (hiccup/html entry)) tag)))))
      (str acc li-close)))))


(def _list-types {true ["<ol>" "</ol>"] :ol ["<ol>" "</ol>"] :ul ["<ul>" "</ul>"]})
(defn generate-toc [^String html & {list-type :list-type :or {list-type true}}]
  "Reads an HTML string and parses it for headers, then returns a list of links
  to them.

  Optionally, a map of :list-type can be provided with value :ul, :ol, or true.
  :ol and true will result in an ordered list being generated for the table of
  contents, while :ul will result in an unordered list. The default is an
  ordered list."
  (let [li-tags (list-type _list-types)
        top-tag (li-tags -> first (subs 1 3))]
    (-> html
        (.getBytes "UTF-8")
        (java.io.ByteArrayInputStream.)
        (html/parse)
        :content
        (get-headings)
        (make-links li-tags)
        (clojure.string/replace-first
          (re-pattern top-tag) (str top-tag "class=\"contents\"")))))
