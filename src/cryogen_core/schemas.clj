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
  {(s/optional-key :layout)     s/Keyword
   :title                       s/Str
   (s/optional-key :date)       s/Str
   (s/optional-key :author)     s/Str
   (s/optional-key :tags)       [s/Str]
   (s/optional-key :toc)        (s/conditional keyword? (s/enum :ol :ul)
                                               :else s/Bool)
   (s/optional-key :draft?)     s/Bool
   (s/optional-key :klipse)     (s/conditional map? Klipse
                                               :else (s/pred true?))
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
   (s/optional-key :public-dest)          s/Str
   :blog-prefix                           s/Str
   :rss-name                              s/Str
   (s/optional-key :rss-filters)          [s/Str]
   (s/optional-key :recent-posts)         s/Int
   :post-date-format                      s/Str
   (s/optional-key :archive-group-format) s/Str
   (s/optional-key :sass-src)             [s/Str]
   (s/optional-key :sass-path)            s/Str
   :theme                                 s/Str
   (s/optional-key :resources)            [s/Str]
   (s/optional-key :keep-files)           [s/Str]
   (s/optional-key :disqus?)              s/Bool
   (s/optional-key :disqus-shortname)     s/Str
   (s/optional-key :ignored-files)        [s/Regex]
   (s/optional-key :previews?)            s/Bool
   (s/optional-key :posts-per-page)       s/Int
   (s/optional-key :blocks-per-preview)   s/Int
   :clean-urls                            (s/enum :trailing-slash
                                                  :no-trailing-slash
                                                  :dirty)
   (s/optional-key :collapse-subdirs?)    s/Bool
   (s/optional-key :hide-future-posts?)   s/Bool
   (s/optional-key :klipse)               Klipse
   (s/optional-key :debug?)               s/Bool
   (s/optional-key :copy-html)            [s/Str]
   (s/optional-key :compile-html)         [s/Str]
   s/Keyword                              s/Any})
