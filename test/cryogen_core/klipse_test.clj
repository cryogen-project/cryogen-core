(ns cryogen-core.klipse-test
  (:require [cryogen-core.klipse :refer :all]
            [clojure.test :refer [deftest testing is are]]))

(deftest map-keys-test
  (is (= {"a" 1 "b" 2} (map-keys name {:a 1 :b 2}))))

(deftest update-existing-test
  (is (= {:a 1 :b 2} (update-existing {:a 1 :b 1} :b inc)))
  (is (= {:a 1}      (update-existing {:a 1} :b (constantly 2)))))

(deftest deep-merge-test
  (is (= {:a {:b 1 :c 2}}  (deep-merge {:a {:b 1}} {:a {:c 2}})))
  (is (= {:a {:b 1}}       (deep-merge {:a {:b 1}} {:a nil})))
  (is (= {:a {:b 1 :c 3}}  (deep-merge {:a {:b 1 :c 2}} {:a {:c 3}}))))

(deftest code-block-classes-test
  (is (= ["clojure" "ruby"]
         (code-block-classes
          "<h1>stuff</h1>
<div class=\"not-code\"><pre><code class=\"clojure\">(def x 42)</code></pre></div>
<pre><code class=\"ruby\">123</code><pre>"))))

(deftest clojure-eval-classes-test
  (is (= #{"eval-cljs" "eval-reagent"}
         (clojure-eval-classes {"selector" ".eval-cljs"
                                "selector_reagent" ".eval-reagent"
                                "selector_eval_ruby" ".eval-ruby"}))))

(deftest clojure-eval?-test
  (is (clojure-eval? {"selector" ".eval-cljs"}
                     "<h1>stuff</h1>
<div class=\"not-code\"><pre><code class=\"eval-cljs\">(def x 42)</code></pre></div>
<pre><code class=\"ruby\">123</code><pre>"))

  (is (not (clojure-eval? {"selector" ".eval-cljs"
                           "selector_eval_ruby" ".eval-ruby"}
                          "<h1>stuff</h1>
<pre><code class=\"eval-ruby\">123</code><pre>"))))

(deftest normalize-settings-test
  (is (= {"selector_reagent" ".reagent"
          "codemirror_options_in" {"lineNumbers" true}}
         (normalize-settings
          {:selector-reagent ".reagent"
           :codemirror-options-in {:line-numbers true}}))))

(deftest merge-configs-test
  (testing "Things are merged correctly"
    (is (= (merge defaults
                  {:settings {"selector" ".clojure-eval"
                              "codemirror_options_in" {"lineNumbers" true}}})
           (merge-configs {:settings {:codemirror-options-in {:line-numbers true}}}
                          {:settings {:selector ".clojure-eval"}}))))

  (testing "If it's all set up in config.edn, in the post it can be just :klipse true"
    (is (= (merge defaults {:settings {"selector_js" ".javascript"}})
           (merge-configs {:settings {:selector-js ".javascript"}} true)))))

(def valid-cfg
  "A minimal valid config."
  {:settings {:selector ".cljs"}})

(deftest klipsify?-test
  (is (false? (klipsify? {} true)))
  (is (false? (klipsify? {} {})))
  (is (false? (klipsify? nil {})))
  (is (false? (klipsify? valid-cfg nil)))
  (is (true? (klipsify? valid-cfg {})))
  (is (true? (klipsify? valid-cfg true)))
  (is (true? (klipsify? {} valid-cfg)))
  (is (true? (klipsify? nil valid-cfg))))
