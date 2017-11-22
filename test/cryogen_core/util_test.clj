(ns cryogen-core.util-test
  (:require [cryogen-core.util :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [clojure.test :refer [deftest testing is are]]))

(deftest filter-html-elems-test
  (is (= (enlive/html [:div {:class "x"} [:div {:class "x"} "foo"]]
                      [:div {:class "x"} "foo"]))
      (filter-html-elems (comp #{"x"} :class :attrs)
                         (enlive/html
                          [:h1 {:class "y"} "things!"]
                          [:div {:class "x"} [:div {:class "x"} "foo"]]))))
