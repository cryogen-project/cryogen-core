(ns cryogen-core.sitemap
  (:require [clojure.xml :refer [emit]]
            [cryogen-core.io :as cryogen-io])
  (:import java.util.Date))

;;generate sitemaps using the sitemap spec
;;http://www.sitemaps.org/protocol.html

(defn format-date [date]
  (let [fmt (java.text.SimpleDateFormat. "yyyy-MM-dd")]
    (.format fmt date)))

(defn loc [^java.io.File f]
  (-> f (.getAbsolutePath) (.split (cryogen-io/path cryogen-io/public "/")) second))

(defn generate [site-url ignored-files]
  (with-out-str
    (emit
      {:tag :urlset
       :attrs {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
       :content
       (for [^java.io.File f (cryogen-io/find-assets cryogen-io/public ".html" ignored-files)]
         {:tag :url
          :content
          [{:tag :loc
            :content [(str site-url (loc f))]}
           {:tag :lastmod
            :content [(-> f (.lastModified) (Date.) format-date)]}]})})))
