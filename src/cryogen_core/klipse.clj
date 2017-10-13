(ns cryogen-core.klipse
  (:require
    [camel-snake-kebab.core :refer [->snake_case_string ->camelCaseString]]
    [cheshire.core :as json]
    [clojure.string :as string]
    [net.cgrand.enlive-html :as enlive]))

;;;;;;;;;;;
;; utils

(defn map-keys
  "Applies f to each key in m"
  [f m]
  (zipmap (map f (keys m)) (vals m)))

(defn update-existing
  "Like clojure.core/update, but returns m untouched if it doesn't contain k"
  [m k f & args]
  (if (contains? m k)
    (apply update m k f args)
    m))

(def map-or-nil? (some-fn map? nil?))

(defn deep-merge
  "Like clojure.core/merge, but also merges nested maps under the same key."
  [& ms]
  (apply merge-with
         (fn [v1 v2]
           (if (and (map-or-nil? v1) (map-or-nil? v2))
             (deep-merge v1 v2)
             v2))
         ms))

(defn filter-html-elems
  "Recursively walks a sequence of enlive-style html elements depth first
  and returns a flat sequence of the elements where (pred elem)"
  [pred html-elems]
  (reduce (fn [acc {:keys [content] :as elem}]
            (into (if (pred elem) (conj acc elem) acc)
                  (filter-html-elems pred content)))
          [] html-elems))

(defn code-block-classes
  "Takes a string of html and returns a sequence of
  all the classes on all code blocks."
  [html]
  (->> html
       enlive/html-snippet
       (filter-html-elems (comp #{:code} :tag))
       (keep (comp :class :attrs))
       (mapcat #(string/split % #" "))))

;;;;;;;;;;;;
;; klipse

(defn eval-classes
  "Takes the :settings map and returns all values that are css class selectors."
  [settings]
  (filter #(string/starts-with? % ".") (vals settings)))

(defn tag-nohighlight
  "Takes html as a string and a coll of class-selectors and adds
   nohighlight to all code blocks that includes one of them."
  [html settings]
  (letfn [(tag [h clas]
            (enlive/sniptest h
                             [(keyword (str "code" clas))]
                             (fn [x]
                               (update-in x [:attrs :class] #(str % " nohighlight")))))]
    (reduce tag html (eval-classes settings))))

(def defaults
  {:js-src   {:min     "https://storage.googleapis.com/app.klipse.tech/plugin_prod/js/klipse_plugin.min.js"
              :non-min "https://storage.googleapis.com/app.klipse.tech/plugin/js/klipse_plugin.js"}
   :css-base "https://storage.googleapis.com/app.klipse.tech/css/codemirror.css"})

;; This needs to be updated whenever a new clojure selector is introduced.
;; It should only be necessary for react wrappers and the like, so not very often.
;; When (if?) self hosted cljs becomes compatible with advanced builds
;; this can be removed and we can just always use minified js.
(def clojure-selectors
  "A set of selectors that imply clojure evaluation."
  #{"selector" "selector_reagent"})

(defn clojure-eval-classes
  "Takes settings and returns a set of the html classes that imply clojure eval."
  [normalized-settings]
  (reduce (fn [classes selector]
            (if-let [klass (get normalized-settings selector)]
              (conj classes (->> klass rest (apply str)))   ;; Strip the leading .
              classes))
          #{} clojure-selectors))

(defn clojure-eval?
  "Takes settings and html and returns whether there is any clojure eval."
  [normalized-settings html]
  (boolean (some (clojure-eval-classes normalized-settings) (code-block-classes html))))

(defn normalize-settings
  "Transform the keys to the correct snake-case or camelCase strings."
  [settings]
  (-> (map-keys ->snake_case_string settings)
      (update-existing "codemirror_options_in" (partial map-keys ->camelCaseString))
      (update-existing "codemirror_options_out" (partial map-keys ->camelCaseString))))

(defn merge-configs
  "Merges the defaults, global config and post config,
  transforms lisp-case keywords into snake_case/camelCase strings
  Returns nil if there's no post-config.
  A post-config with the value true counts as an empty map."
  [global-config post-config]
  (when post-config
    (let [post-config (if (true? post-config) {} post-config)]
      (deep-merge defaults
                  (update-existing global-config :settings normalize-settings)
                  (update-existing post-config :settings normalize-settings)))))

(defn infer-clojure-eval
  "Infers whether there's clojure eval and returns the config with the
  appropriate value assoc'd to :js.
  Returns the config untouched if :js is already specified."
  [config html]
  (if (:js config)
    config
    (assoc config
      :js
      (if (clojure-eval? (:settings config) html) :non-min :min))))

(defn include-css [href]
  (str "<link rel=\"stylesheet\" type=\"text/css\" href=" (pr-str href) ">"))

(defn include-js [src]
  (str "<script src=" (pr-str src) "></script>"))

(defn emit
  "Takes the :klipse config from config.edn and the :klipse config from the
  current post, and returns the html to include on the bottom of the page."
  [config html]
  (when-let [{:keys [settings js-src js css-base css-theme]}
             (infer-clojure-eval config html)]

    (str (include-css css-base) "\n"
         (when css-theme (str (include-css css-theme) "\n"))
         "<script>\n"
         "window.klipse_settings = " (json/generate-string settings {:pretty true}) ";\n"
         "</script>\n"
         (include-js (js js-src)))))
