(ns cryogen-core.util-test
  (:require [cryogen-core.util :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [clojure.test :refer [deftest testing is are]]))

(deftest filter-html-elems-test
  (is (= (enlive/html [:div {:class "x"} [:div {:class "x"} "foo"]]
                      [:div {:class "x"} "foo"])
         (filter-html-elems (comp #{"x"} :class :attrs)
                            (enlive/html
                             [:h1 {:class "y"} "things!"]
                             [:div {:class "x"} [:div {:class "x"} "foo"]])))))

(deftest conj-some-test
  (is (= [1 2 3] (conj-some [] nil 1 nil 2 nil 3 nil))))

(deftest hic=-test
  (is (hic= [:div.foo [:p "bar"]]
            [:div {:class "foo"} '([:p "bar"])])))

(deftest enlive->hiccup-test
  (is (hic= [:div.foo [:p "bar"]]
            (enlive->hiccup {:tag :div,
                             :attrs {:class "foo"},
                             :content '({:tag :p :content "bar"})}))))
