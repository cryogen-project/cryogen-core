(ns cryogen-core.util
  (:require [net.cgrand.enlive-html :as enlive]
            [hiccup.core :as hiccup]))

(defn filter-html-elems
  "Recursively walks a sequence of enlive-style html elements depth first
  and returns a flat sequence of the elements where (pred elem)"
  [pred html-elems]
  (reduce (fn [acc {:keys [content] :as elem}]
            (into (if (pred elem) (conj acc elem) acc)
                  (filter-html-elems pred content)))
          [] html-elems))

(defn conj-some
  "Like conj, but ignores xs that are nil."
  ([] [])
  ([coll] coll)
  ([coll & xs]
   (apply conj coll (remove nil? xs))))

(defn enlive->hiccup
  "Transform enlive style html to hiccup.
  Can take (and return) either a node or a sequence of nodes."
  [node-or-nodes]
  (cond (string? node-or-nodes) node-or-nodes
        (empty? node-or-nodes) nil
        (sequential? node-or-nodes) (map enlive->hiccup node-or-nodes)
        :else (let [{:keys [tag attrs content]} node-or-nodes]
                (conj-some [tag] (not-empty attrs) (enlive->hiccup content)))))

(defn enlive->html-text [node-or-nodes]
  (->> node-or-nodes
       (enlive/emit*)
       (apply str)))

(defn enlive->plain-text [node-or-nodes]
  (->> node-or-nodes
       (enlive/texts)
       (apply str)))

(defn hic=
  "Tests whether the xs are equivalent hiccup."
  [x & xs]
  (apply = (map #(hiccup/html %) (cons x xs))))
