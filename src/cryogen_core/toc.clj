(ns cryogen-core.toc
  (:require [crouton.html :as html]
            [hiccup.core :as hiccup]))

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


(defn- parse [{tag :tag {id :id} :attrs [{{name :name} :attrs} title :as htext] :content}]
  (hash-map :anchor (or id name)
            :tag tag 
            :title (or title (first htext))))

(defn build-toc [hs]
  "Creates a table of contents from the given headings in form of clojure structure for hiccup. 
  This function will look  for either:
  (1) headings with a child anchor with a non-nil name attribute, e.g.
      <h1><a name=\"reference\">Reference Title</a></h1>
  or
  (2) headings with an id attribute, e.g. <h1 id=\"reference\">Reference Title</h1>
  In both cases above, the anchor reference becomes \"#reference\" and the
  anchor text is \"Reference Title\"."  
  (let [actual (filterv #(not (nil? (:anchor %))) (map parse (reverse hs))) ;from last to first
        tags (vec (sort (distinct (map :tag actual))))] ;expecting only h1..h6 tags 
    (loop [[head & tail] actual toc [:ol.contents ] _prev 0]
      (let [depth (.indexOf levels (:tag head))
            entry [:li [:a {:href (str "#" (head :anchor))} (head :title)]]
            laddr (repeat (min depth _prev) 1)       ;cannot dig more than previous entry level
            wrap (loop [cnt (max 0 (- depth _prev))  ;add more :ol around entry if we need
                           acc entry] 
                      (if (= 0 cnt) acc (recur (dec cnt) (conj [:ol] acc))))
            add-h #(into [:ol hood] (rest %))        ;add new entry before others
            ntoc (if (empty? laddr)                  ;on necessary level
                    (add-h toc)
                    (update-in toc laddr add-h))] 
        (if (empty? tail) 
          ntoc
          (recur tail ntoc depth))))))


(defn generate-toc [html]
  (-> html
      (.getBytes "UTF-8")
      (java.io.ByteArrayInputStream.)
      (html/parse)
      :content
      (get-headings)
      (build-toc)
      (hiccup/html)))
