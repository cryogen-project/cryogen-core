(ns cryogen-core.markup
  (:require [clojure.string :as string]))

(defonce markup-registry (atom []))

(defprotocol Markup
  "A markup engine comprising a dir(ectory) containing markup files,
  an ext(ension) for finding markup file names, and a render-fn that returns
  a fn with the signature [java.io.Reader config] -> String (HTML)."
  (dir [this])
  (ext [this])
  (render-fn [this]))

(defn rewrite-hrefs
  "Injects the blog prefix in front of any local links
    ex. <img src='/img/cryogen.png'/> becomes <img src='/blog/img/cryogen.png'/>"
  [blog-prefix text]
  (if (string/blank? blog-prefix)
    text
    (string/replace text
                    #"(?!href=.?//)href=.?/|(?!src=.?//)src=.?/"
                    #(str (subs % 0 (dec (count %))) blog-prefix "/"))))

(defn markups
  "Return a vector of Markup implementations. This is the primary entry point
  for a client of this ns. This vector should be used to iterate over supported
  Markups."
  []
  @markup-registry)

(defn register-markup
  "Add a Markup implementation to the registry."
  [mu]
  (swap! markup-registry conj mu))

(defn clear-registry
  "Reset the Markup registry."
  []
  (reset! markup-registry []))
