(ns cryogen-core.markup
  (:require [markdown.core :refer [md-to-html-string]]
            [markdown.transformers :refer [transformer-vector]]
            [clojure.string :as s])
  (:import org.asciidoctor.Asciidoctor$Factory
           java.util.Collections))

(defprotocol Markup
  "A markup engine comprising a dir(ectory) containing markup files,
  an ext(ension) for finding markup file names, and a render-fn that returns
  a fn with the signature [java.io.Reader config] -> String (HTML)."
  (dir [this])
  (ext [this])
  (render-fn [this]))

(defn- rewrite-hrefs
  "Injects the blog prefix in front of any local links

  ex. <img src='/img/cryogen.png'/> becomes <img src='/blog/img/cryogen.png'/>"
  [blog-prefix text]
  (clojure.string/replace text #"href=.?/|src=.?/" #(str (subs % 0 (dec (count %))) blog-prefix "/")))

(defn- rewrite-hrefs-transformer
  "A :replacement-transformer for use in markdown.core that will inject the
  given blog prefix in front of local links."
  [{:keys [blog-prefix]} text state]
  [(rewrite-hrefs blog-prefix text) state])

(defn- markdown
  "Returns a Markdown (https://daringfireball.net/projects/markdown/)
  implementation of the Markup protocol."
  []
  (reify Markup
    (dir [this] "md")
    (ext [this] ".md")
    (render-fn [this]
      (fn [rdr config]
        (md-to-html-string
          (->> (java.io.BufferedReader. rdr)
            (line-seq)
            (s/join "\n"))
          :reference-links? true
          :heading-anchors true
          :replacement-transformers (conj transformer-vector (partial rewrite-hrefs-transformer config)))))))

(defn- asciidoc
  "Returns an Asciidoc (http://asciidoc.org/) implementation of the
  Markup protocol."
  []
  (reify Markup
    (dir [this] "asc")
    (ext [this] ".asc")
    (render-fn [this]
      (let [attributes (java.util.HashMap. {"toc" "macro"})
            options    (java.util.HashMap. {"attributes" attributes})]
        (fn [rdr _]
          (.convert (Asciidoctor$Factory/create)
                    (->> (java.io.BufferedReader. rdr)
                         (line-seq)
                         (s/join "\n"))
                    options))))))

(defn markups
  "Return a vector of Markup implementations. This is the primary entry point
  for a client of this ns. This vector should be used to iterate over supported
  Markups."
  []
  [(markdown) (asciidoc)])
