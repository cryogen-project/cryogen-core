(ns cryogen-core.klipse
  (:require
   [camel-snake-kebab.core :refer [->snake_case_string ->camelCaseString]]
   [cheshire.core :as json]))

;;;;;;;;;;;
;; utils

(defn map-keys
  "Applies f to each key in m"
  [f m]
  (zipmap (map f (keys m)) (vals m)))

(defn update-existing
  "Like clojure.core/update, but returns m untouched if it doesn't contain k"
  [m k f & args]
  (if (contains? m k) (apply update m k f args) m))

(def map-or-nil? (some-fn map? nil?))

(defn  deep-merge
  "Like clojure.core/merge, but also merges nested maps under the same key."
  [& ms]
  (apply merge-with
         (fn [v1 v2]
           (if (and (map-or-nil? v1) (map-or-nil? v2))
             (deep-merge v1 v2)
             v2))
         ms))

;;;;;;;;;;;;
;; klipse

(def defaults
  {:js-src
   {:min "https://storage.googleapis.com/app.klipse.tech/plugin_prod/js/klipse_plugin.min.js"
    :non-min "https://storage.googleapis.com/app.klipse.tech/plugin/js/klipse_plugin.js"}

   :css-base "https://storage.googleapis.com/app.klipse.tech/css/codemirror.css"})

;; This needs to be updated whenever a new clojure selector is introduced.
;; It should only be necessary for react wrappers and the like, so not very often.
;; When (if?) self hosted cljs becomes compatible with advanced builds
;; this can be removed and we can just always use minified js.
(def clojure-selectors
  "A set of selectors that imply clojure evaluation."
  #{"selector" "selector_reagent"})

(defn clojure-eval?
  "Do the settings include any keys that imply clojure eval?"
  [normalized-settings]
  (some clojure-selectors (keys normalized-settings)))

(defn normalize-settings
  "Transform the keys to the correct snake-case or camelCase strings."
  [settings]
  (-> (map-keys ->snake_case_string settings)
      (update-existing "codemirror_options_in" (partial map-keys ->camelCaseString))
      (update-existing "codemirror_options_out" (partial map-keys ->camelCaseString))))

(defn merge-configs
  "Merges the defaults, global config and post config,
  transforms lisp-case keywords into snake_case/camelCase strings
  and infers whether to use minified or non-minified js
  (this can be overridden by supplying a :js key).
  If the post config is just true, uses only the global config.
  Returns nil if either there's no post-config or if there's no settings after merging."
  [global-config post-config]
  (when post-config
    (let [post-config (if (true? post-config) {} post-config)
          merged-config (deep-merge defaults
                                    (update-existing global-config :settings normalize-settings)
                                    (update-existing post-config :settings normalize-settings))]
      ;; It would make more sense to return nil if there's no selector specified
      ;; instead of no settings, but that would be too much of a
      ;; maintenance burden, as there are new selectors added all the time.
      (when (:settings merged-config)
        (if (:js merged-config)
          merged-config
          (assoc merged-config :js (if (clojure-eval? (:settings merged-config))
                                     :non-min
                                     :min)))))))

(defn include-css [href]
  (str "<link rel=\"stylesheet\" type=\"text/css\" href=" (pr-str href) ">"))

(defn include-js [src]
  (str "<script src=" (pr-str src) "></script>"))

(defn emit
  "Takes the :klipse config from config.edn and the :klipse config from the
  current post, and returns the html to include on the bottom of the page."
  [global-config post-config]
  (when-let [{:keys [settings js-src js css-base css-theme]}
             (merge-configs global-config post-config)]

    (assert (#{:min :non-min} js)
            (str ":js needs to be one of :min or :non-min but was: " js))

    (str (include-css css-base) "\n"
         (when css-theme (str (include-css css-theme) "\n"))
         "<script>\n"
         "window.klipse_settings = " (json/generate-string settings {:pretty true}) ";\n"
         "</script>\n"
         (include-js (js js-src)))))
