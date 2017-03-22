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
      
(deftest test-normalized-page-root-uri
  (testing      
    (is (= "" (sut/normalized-page-root-uri "")))
    (is (= "/root/" (sut/normalized-page-root-uri "root")))
    (is (= "/root/" (sut/normalized-page-root-uri "/root")))
    (is (= "/root/" (sut/normalized-page-root-uri "root/")))
    (is (= "/root/" (sut/normalized-page-root-uri "/root/")))
    ))

(deftest test-uri-level
  (testing      
    (is (= 2 (sut/uri-level "/pages/nav1/")))
    (is (= 2 (sut/uri-level "/pages/nav1.html")))
    ))

(def pages-clean-1 [(page "/pages/nav1/" 0) 
                    (page "/pages/nav1/nav11/" 1)
                    (page "/pages/nav1/nav13/" 3)
                    (page "/pages/nav1/nav11/nav112/" 2)
                    (page "/pages/nav1/nav12/" 2)
                    (page "/pages/nav1/nav11/xnav111/" 1)
                    ])

(def expected-clean-1 [(enhanced-page 
                         "/pages/nav1/" 0
                         [(enhanced-page 
                            "/pages/nav1/nav11/" 1
                            [(page "/pages/nav1/nav11/xnav111/" 1)
                             (page "/pages/nav1/nav11/nav112/" 2)])
                          (page "/pages/nav1/nav12/" 2)
                          (page "/pages/nav1/nav13/" 3)]
                         )])

(def pages-dirty [(page "/pages/nav1.html" 0) 
                  (page "/pages/nav1/nav11.html" 1)
                  (page "/pages/nav1/nav13.html" 3)
                  (page "/pages/nav1/nav11/nav112.html" 2)
                  (page "/pages/nav1/nav12.html" 2)
                  (page "/pages/nav1/nav11/xnav111.html" 1)
                  ])

(def expected-dirty [(enhanced-page 
                       "/pages/nav1.html" 0
                       [(enhanced-page 
                          "/pages/nav1/nav11.html" 1
                          [(page "/pages/nav1/nav11/xnav111.html" 1)
                           (page "/pages/nav1/nav11/nav112.html" 2)])
                        (page "/pages/nav1/nav12.html" 2)
                        (page "/pages/nav1/nav13.html" 3)]
                       )])

(def pages-clean-2 [(page "/pages/1/" 0) 
                    (page "/pages/2/" 1)
                    (page "/pages/2/22/" 0)])

(def expected-clean-2 [(page "/pages/1/" 0)
                       (enhanced-page 
                         "/pages/2/" 1
                         [(page "/pages/2/22/" 0)])])

(def pages-clean-3 [(page "/1/" 0) 
                    (page "/2/" 1)
                    (page "/2/22/" 0)])

(def expected-clean-3 [(page "/1/" 0)
                       (enhanced-page 
                         "/2/" 1
                         [(page "/2/22/" 0)])])

(deftest test-hierarchic-pages
  (testing 
    "hierarchic expectations"        
      (is (= expected-clean-1
             (sut/build-hierarchic-map "pages" pages-clean-1)))
      (is (= expected-dirty
             (sut/build-hierarchic-map "pages" pages-dirty)))
      (is (= expected-clean-2
             (sut/build-hierarchic-map "pages" pages-clean-2)))
      (is (= expected-clean-3
             (sut/build-hierarchic-map "" pages-clean-3)))
      )
    )

(deftest test-filter-pages-for-uri
  (testing      
    (is (= 6 (count (sut/filter-pages-for-uri "/pages/nav1/" pages-clean-1))))
    (is (= 6 (count (sut/filter-pages-for-uri "/pages/nav1.html" pages-dirty))))
    ))