(ns cryogen-core.rss
  (:require [clj-rss.core :as rss]
            [clojure.xml :refer [emit]]
            [text-decoration.core :refer :all])
  (:import java.util.Date))


(defn posts-to-items [site-url posts]
  (map
    (fn [{:keys [uri title content date]}]
      (let [link (str (if (.endsWith site-url "/") (apply str (butlast site-url)) site-url) uri)]
        {:guid        link
         :link        link
         :title       title
         :description content
         :pubDate     date}))
    posts))

(defn make-channel [config posts]
  (apply
    (partial rss/channel-xml
             false
             {:title         (:site-title config)
              :link          (:site-url config)
              :description   (:description config)
              :lastBuildDate (Date.)})
    (posts-to-items (:site-url config) posts)))

(defn make-filtered-channels [public {:keys [rss-filters blog-prefix] :as config} posts-by-tag]
  (doseq [filter rss-filters]
    (let [uri (str public blog-prefix "/" (name filter) ".xml")]
      (println "\t-->" (cyan uri))
      (spit uri (make-channel config (get posts-by-tag filter))))))
