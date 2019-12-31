(ns cryogen-core.zip-util-test
  (:require [clojure.test :refer :all]
            [clojure.zip :as z]
            [net.cgrand.enlive-html :as enlive]
            [cryogen-core.zip-util :refer :all]))

(def tree [1 [21 22 23] 3])

(defn treez [] (z/vector-zip tree))

(defn html-zipper [s]
  (z/xml-zip {:content (enlive/html-snippet s)}))

(deftest find-nearest-left-test
  (is (= (z/node (find-nearest-left (find-node (treez) #{22})))
         21))
  (is (= (z/node (find-nearest-left (find-node (treez) #{21})))
         1))
  (is (nil? (find-nearest-left (find-node (treez) #{1})))))

(deftest more-marker?-test
  (is (= (-> (find-node
               (html-zipper "<div><p>first</p><p>second<!--more--></p></div><p>after</p>")
               more-marker?)
             (z/left)
             (z/node))
         "second")))

(deftest cut-tree-vertically-test
  (is (nil? (cut-tree-vertically (treez) #{1})))
  (is (nil? (cut-tree-vertically (treez) #{:no-such-node})))
  (is (= (z/node (cut-tree-vertically (treez) #{21}))
         [1]))
  (is (= (z/node (cut-tree-vertically (treez) #{22}))
         [1 [21]]))
  (is (= (z/node (cut-tree-vertically (treez) #{23}))
         [1 [21 22]])))

