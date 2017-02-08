(ns cryogen-core.navbar-model-test
  (:require 
    [clojure.test :refer :all]
    [cryogen-core.navbar-model :as sut]))

(defn- page [uri page-index] 
  {:uri uri 
   :content uri 
   :page-index page-index})

(defn- enhanced-page [uri page-index children] 
  {:uri uri 
   :content uri
   :page-index page-index
   :children children})
           
(deftest test-navmap-pages
  (testing 
    "No pages or posts nothing to copy"
    (let [pages [(page "/pages/nav1/" 0) 
                 (page "/pages/nav1/nav11/" 1)
                 (page "/pages/nav1/nav13/" 3)
                 (page "/pages/nav1/nav11/nav112/" 2)
                 (page "/pages/nav1/nav12/" 2)
                 (page "/pages/nav1/nav11/xnav111/" 1)
                 ]
          expected [(enhanced-page 
                      "/pages/nav1/" 0
                      [(enhanced-page 
                         "/pages/nav1/nav11/" 1
                         [(page "/pages/nav1/nav11/xnav111/" 1)
                          (page "/pages/nav1/nav11/nav112/" 2)])
                       (page "/pages/nav1/nav12/" 2)
                       (page "/pages/nav1/nav13/" 3)]
                      )] 
          ]        
      (is (= expected
             (sut/build-nav-map pages)))
      )
    ))
