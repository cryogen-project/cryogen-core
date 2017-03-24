(ns cryogen-core.schemas
  (:require
    [schema.core :as s]))

(def Klipse
  {(s/optional-key :settings)  s/Any
   (s/optional-key :js-src)    {:min s/Str :non-min s/Str}
   (s/optional-key :js)        (s/enum :min :non-min)
   (s/optional-key :css-base)  s/Str
   (s/optional-key :css-theme) s/Str})

(def MetaData
  {:layout                      s/Keyword
   :title                       s/Str
   (s/optional-key :date)       s/Str
   (s/optional-key :author)     s/Str
   (s/optional-key :tags)       [s/Str]
   (s/optional-key :toc)        s/Bool
   (s/optional-key :draft?)     s/Bool
   (s/optional-key :klipse)     Klipse
   (s/optional-key :home?)      s/Bool
   (s/optional-key :page-index) s/Int
   (s/optional-key :navbar?)    s/Bool
   s/Keyword                    s/Any})

(def Config
  {:site-title                            s/Str
   :author                                s/Str
   :description                           s/Str
   :site-url                              s/Str
   :post-root                             s/Str
   :page-root                             s/Str
   (s/optional-key :post-root-uri)        (s/maybe s/Str)
   (s/optional-key :page-root-uri)        (s/maybe s/Str)
   (s/optional-key :tag-root-uri)         s/Str
   (s/optional-key :author-root-uri)      s/Str
   :blog-prefix                           s/Str
   :rss-name                              s/Str
   :rss-filters                           [s/Str]
   :recent-posts                          s/Int
   :post-date-format                      s/Str
   (s/optional-key :archive-group-format) s/Str
   (s/optional-key :sass-src)             s/Str
   (s/optional-key :sass-path)            s/Str
   (s/optional-key :compass-path)         s/Str
   :theme                                 s/Str
   :resources                             [s/Str]
   :keep-files                            [s/Str]
   :disqus?                               s/Bool
   (s/optional-key :disqus-shortname)     s/Str
   :ignored-files                         [s/Regex]
   :previews?                             s/Bool
   (s/optional-key :posts-per-page)       s/Int
   (s/optional-key :blocks-per-preview)   s/Int
   :clean-urls?                           s/Bool
   :hide-future-posts?                    s/Bool
   (s/optional-key :klipse)               Klipse
   :debug?                                s/Bool
   s/Keyword                              s/Any})