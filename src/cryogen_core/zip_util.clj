(ns cryogen-core.zip-util
  "Utils for working with Clojure Zippers"
  (:require [clojure.zip :as zip]))

(defn remove-rights
  "Remove all nodes to the right of this one without moving.
  Modelled after `clojure.zip/insert-right`"
  [loc]
  (let [[node {r :r :as path}] loc]
    (if (nil? path)
      (throw (new Exception "Remove siblings at top"))
      (with-meta [node (assoc path :r [] :changed? true)] (meta loc)))))

;; (defn pr-nodes [nodes]
;;   (->> nodes (map-indexed #(prn %1 ": " (->> (str %2) (take 40) (apply str)))) dorun))
;;
;; (defn pr-rights [loc]
;;   (->> loc zip/rights pr-nodes))

(defn root? [loc] (nil? (zip/path loc)))

(defn remove-rights-til-root [loc]
  (if (root? loc)
    loc
    (recur (try
             (zip/up (remove-rights loc))
             (catch Exception e
               (throw (ex-info (ex-message e) {:loc loc})))))))

(defn find-node
  "Return the `loc` of the tree that the `pred` returns true for or nil.
  The pred is invoked with the node (`zip/node`)."
  [loc pred]
  (cond
    (zip/end? loc) nil
    (pred (zip/node loc)) loc
    :else (recur (zip/next loc) pred)))

(defn find-nearest-left
  "Find the nearest left loc of the given `loc`. If it is the leftmost one,
  search up until you find a node that has a left neighbour. Return nil if none."
  [loc]
  (when-not (root? loc)
    (or (zip/left loc)
        (recur (zip/up loc)))))

(defn more-marker?
  "Is the given DOM node the `<!--more-->` marker?"
  [node]
  (= node {:type :comment, :data "more"}))

(defn cut-tree-vertically
  "Given a Zipper, find a node matching the `pred` and cut the tree vertically
  there (effectively removing all content but closing tags).
  Returns the root of the tree. Returns nil if `pred` matches nothing."
  [loc pred]
  {:pre [loc pred]}
  (some-> loc
          (find-node pred)
          (find-nearest-left)
          (remove-rights-til-root)))