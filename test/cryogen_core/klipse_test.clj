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

(deftest normalize-settings-test
  (is (= {"selector_reagent" ".reagent"
          "codemirror_options_in" {"lineNumbers" true}}
         (normalize-settings
          {:selector-reagent ".reagent"
           :codemirror-options-in {:line-numbers true}}))))

(deftest merge-configs-test
  (testing "Things are merged correctly, and :js :non-min is inferred from :selector."
    (is (= (merge defaults
                  {:settings {"selector" ".clojure"
                              "codemirror_options_in" {"lineNumbers" true}}
                   :js :non-min})
           (merge-configs {:settings {:codemirror-options-in {:line-numbers true}}}
                          {:settings {:selector ".clojure"}}))))

  (testing "If it's all set up in config.edn, in the post it can be just :klipse true"
    (is (= (merge defaults
                  {:settings {"selector_js" ".javascript"}
                   :js :min})
           (merge-configs {:settings {:selector-js ".javascript"}} true))))

  (testing "Returns nil if there's nothing in the blog post"
    (is (nil? (merge-configs {:settings {:selector ".clojure"}} nil))))

  (testing "If there's no :settings, returns nil"
    (is (nil? (merge-configs {:css-base "/css/base.css"} {:css-theme "/css/theme.css"})))))
