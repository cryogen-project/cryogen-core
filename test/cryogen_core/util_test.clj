(ns cryogen-core.util-test
  (:require [clojure.test :refer [deftest is]]
            [cryogen-core.util :refer :all]
            [net.cgrand.enlive-html :as enlive]) 
  (:import [clojure.lang IExceptionInfo]))

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

(deftest enlive->html-text-test
  (is (= (enlive->html-text (enlive/html-snippet "<p>hi <b>there</b>!</p>"))
         "<p>hi <b>there</b>!</p>")))

(deftest enlive->plain-text-test
  (is (= (enlive->plain-text (enlive/html-snippet "<h1>Greeting:</h1><p>hi <b>there</b>!</p>"))
         "Greeting:hi there!")))

(deftest trimmed-html-snippet-test
  (is (= (trimmed-html-snippet
           "<h1>Hello world</h1>
            <p>Here is some content</p>
            <p>Here is some more content</p>")
         '({:tag :h1, :attrs nil, :content ("Hello world")}
          {:tag :p, :attrs nil, :content ("Here is some content")}
          {:tag :p, :attrs nil, :content ("Here is some more content")})))
  (is (= (trimmed-html-snippet
           "<h1>Hello world</h1><p>Here is some content</p><p>Here is some more content</p>")
         '({:tag :h1, :attrs nil, :content ("Hello world")}
           {:tag :p, :attrs nil, :content ("Here is some content")}
           {:tag :p, :attrs nil, :content ("Here is some more content")})))
  (is (= (trimmed-html-snippet
           "<h1>Hello world</h1>
            <p>Here is some content</p>
            <pre><code>
            this is
              some block formatted
              code
            </code></pre>")
         '({:tag :h1, :attrs nil, :content ("Hello world")}
           {:tag :p, :attrs nil, :content ("Here is some content")}
           {:tag :pre,
            :attrs nil,
            :content ({:tag :code, :attrs nil, :content ("\n            this is\n              some block formatted\n              code\n            ")})}))))

(deftest parse-post-date-test
  (let [expected (.parse (java.text.SimpleDateFormat. "yyyy-mm-dd") "2023-01-02")
        actual (parse-post-date "foo/bar/2023-01-02-froboz.md" "yyyy-mm-dd")]
    (is (= actual expected)))
  (let [expected (.parse (java.text.SimpleDateFormat. "yyyy-mm-dd") "2023-01-02")
        actual (parse-post-date "2023-01-02-froboz.md" "yyyy-mm-dd")]
    (is (= actual expected)))
  (let [formatter (java.text.SimpleDateFormat. "yyyy-mm-dd")
        expected "2023-01-02"
        actual (.format formatter (parse-post-date "foo/bar/2023-02-01-froboz.md" "yyyy-dd-mm"))]
    (is (= actual expected)))
  (is (thrown? Exception (parse-post-date "foo/bar/2023-Jan-02-froboz.md" "yyyy-mm-dd"))
      "Format doesn't match")
  (is (thrown? Exception (parse-post-date "foo/bar/froboz.md" "yyyy-mm-dd"))))