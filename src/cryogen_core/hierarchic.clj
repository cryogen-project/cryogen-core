(ns cryogen-core.hierarchic
  (:require 
    [clojure.string :as s]))

(defn normalized-page-root-uri [page-root-uri]
  (cond 
    (= "" page-root-uri) ""
    (and 
      (s/starts-with? page-root-uri "/")
      (s/ends-with? page-root-uri "/")) page-root-uri
    (and 
      (s/starts-with? page-root-uri "/")
      (not (s/ends-with? page-root-uri "/"))) (str page-root-uri "/")
    (and 
      (not (s/starts-with? page-root-uri "/"))
      (s/ends-with? page-root-uri "/")) (str "/" page-root-uri)
    (and 
      (not (s/starts-with? page-root-uri "/"))
      (not (s/ends-with? page-root-uri "/"))) (str "/" page-root-uri "/"))
  )

(defn uri-level [uri]
  (- (count 
       (s/split uri #"/"))
     1)
  )

(defn filter-pages-for-uri [uri pages]
  (let [html? (s/ends-with? uri ".html")
        clean? (s/ends-with? uri "/")
        clean-uri (cond 
                    html? (subs uri 0 (- (count uri) 5))
                    clean? (subs uri 0 (- (count uri) 1))
                    :default uri)]
    (filter #(s/starts-with? (:uri %) clean-uri) pages))
  )

(defn build-hierarchic-level
  "builds one level of hierarchic tree recurs to next level."
  [parent-uri pages]
  (let [current-level (+ 1 (uri-level parent-uri))
        pages-of-parent (filter-pages-for-uri parent-uri pages)
        pages-on-level (filter #(= current-level (uri-level (:uri %))) pages-of-parent)
        pages-on-child-level (filter #(< current-level (uri-level (:uri %))) pages-of-parent)        
        ]
    (sort-by :page-index
             (map #(let [page-on-level %
              child-pages (filter-pages-for-uri (:uri page-on-level) pages-on-child-level)]
                     (if (empty? child-pages)
                       page-on-level  
                       (merge page-on-level
                              {:children (build-hierarchic-level (:uri page-on-level) child-pages)}))) pages-on-level))
    ))

(defn build-hierarchic-map
  "builds a hierarchic tree from pages"
  [page-root-uri pages]
  (let [sorted-pages (sort-by :uri pages)]
     (build-hierarchic-level (normalized-page-root-uri page-root-uri) sorted-pages)
   ))
