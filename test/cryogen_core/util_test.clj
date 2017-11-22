(ns cryogen-core.util-test
  (:require [cryogen-core.util :refer :all]
            [clojure.test :refer [deftest testing is are]]))

;; For testing convenience.
(defn elt
  "Returns an enlive style html element."
  ([tag] (elt tag nil))
  ([tag attrs & content]
   {:tag tag, :attrs attrs, :content content}))

(deftest filter-html-elems-test
  (is (= [(elt :div {:class "x"} :content [(elt :div {:class "x"} "foo")])
          (elt :div {:class "x"} "foo")])
      (filter-html-elems (comp #{"x"} :class :attrs)
                         [(elt :h1 {:class "y"} "things!")
                          (elt :div {:class "x"} (elt :div {:class "x"} "foo"))])))
