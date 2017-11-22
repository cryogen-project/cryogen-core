(ns cryogen-core.util)

(defn filter-html-elems
  "Recursively walks a sequence of enlive-style html elements depth first
  and returns a flat sequence of the elements where (pred elem)"
  [pred html-elems]
  (reduce (fn [acc {:keys [content] :as elem}]
            (into (if (pred elem) (conj acc elem) acc)
                  (filter-html-elems pred content)))
          [] html-elems))
