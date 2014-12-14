(ns cryogen-core.toc
 (:require [crouton.html :as html]
           [hiccup.core :as hiccup]))

(def _h [:h1 :h2 :h3 :h4 :h5 :h6])
(defn- compare_index [i1 i2] (- (.indexOf _h i2) (.indexOf _h i1)))

(defn get-headings [content]
  (reduce
    (fn [headings {:keys [tag attrs content] :as elm}]
      (if (some #{tag} _h)
        (conj headings elm)
        (if-let [more-headings (get-headings content)]
          (into headings more-headings)
          headings)))
    [] content))

(defn make-links [headings]
  (loop [items headings acc nil _last nil]
    (if-let [{tag :tag [{{name :name} :attrs} title] :content} (first items)]
      (if (nil? name) (recur (rest items) acc nil)
          (let [entry [:li [:a {:href (str "#" name)} title]]
                jump (compare_index _last tag)]
            (cond (> jump 0) (recur (rest items) (str acc "<ol>" (hiccup/html entry)) tag)
                  (= jump 0) (recur (rest items) (str acc (hiccup/html entry)) tag)
                  (< jump 0) (recur (rest items) (str acc (apply str (repeat (* -1 jump) "</ol>"))
                                                      (hiccup/html entry)) tag))))
      (str acc "</ol>"))))

(defn generate-toc [html]
  (-> html
      (.getBytes)
      (java.io.ByteArrayInputStream.)
      (html/parse)
      :content
      (get-headings)
      (make-links)
      (clojure.string/replace-first #"ol" "ol class=\"contents\"")))
