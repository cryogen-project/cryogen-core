(ns cryogen-core.hierarchic-test
  (:require 
    [clojure.test :refer :all]
    [cryogen-core.hierarchic :as sut]))

(defn- page [uri page-index] 
  {:uri uri 
   :content uri 
   :page-index page-index})

(defn- enhanced-page [uri page-index children] 
  {:uri uri 
   :content uri
   :page-index page-index
   :children children})

(def pages-clean [(page "/pages/nav1/" 0) 
                  (page "/pages/nav1/nav11/" 1)
                  (page "/pages/nav1/nav13/" 3)
                  (page "/pages/nav1/nav11/nav112/" 2)
                  (page "/pages/nav1/nav12/" 2)
                  (page "/pages/nav1/nav11/xnav111/" 1)
                  ])

(def pages-dirty [(page "/pages/nav1.html" 0) 
                  (page "/pages/nav1/nav11.html" 1)
                  (page "/pages/nav1/nav13.html" 3)
                  (page "/pages/nav1/nav11/nav112.html" 2)
                  (page "/pages/nav1/nav12.html" 2)
                  (page "/pages/nav1/nav11/xnav111.html" 1)
                  ])
           
(deftest test-uri-level
  (testing      
    (is (= 2 (sut/uri-level "/pages/nav1/")))
    (is (= 2 (sut/uri-level "/pages/nav1.html")))
    ))

(deftest test-filter-pages-for-uri
  (testing      
    (is (= 6 (count (sut/filter-pages-for-uri "/pages/nav1/" pages-clean))))
    (is (= 6 (count (sut/filter-pages-for-uri "/pages/nav1.html" pages-dirty))))
    ))

(deftest test-hierarchic-pages
  (testing 
    "No pages or posts nothing to copy"
    (let [expected-clean [(enhanced-page 
                            "/pages/nav1/" 0
                            [(enhanced-page 
                               "/pages/nav1/nav11/" 1
                               [(page "/pages/nav1/nav11/xnav111/" 1)
                                (page "/pages/nav1/nav11/nav112/" 2)])
                             (page "/pages/nav1/nav12/" 2)
                             (page "/pages/nav1/nav13/" 3)]
                            )]
          expected-dirty [(enhanced-page 
                            "/pages/nav1.html" 0
                            [(enhanced-page 
                               "/pages/nav1/nav11.html" 1
                               [(page "/pages/nav1/nav11/xnav111.html" 1)
                                (page "/pages/nav1/nav11/nav112.html" 2)])
                             (page "/pages/nav1/nav12.html" 2)
                             (page "/pages/nav1/nav13.html" 3)]
                            )] 
          ]        
      (is (= expected-clean
             (sut/build-hierarchic-map pages-clean)))
      (is (= expected-dirty
             (sut/build-hierarchic-map pages-dirty)))
      )
    ))
