(ns cryogen-core.sass
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [text-decoration.core :refer :all]
            [cryogen-core.io :as cryogen-io]))

(defmacro sh
  [& args]
  (let [valid-args (remove nil? args)]
    `(shell/sh ~@valid-args)))

(defn sass-installed?
  "Checks for the installation of Sass."
  [sass-path]
  (zero? (:exit (sh sass-path "--version"))))

(defn compass-installed?
  "Checks for the installation of Compass."
  [compass-path]
  (try
    (zero? (:exit (sh compass-path "--version")))
    (catch java.io.IOException _
      false)))

(defn find-sass-files
  "Given a directory, gets files, filtered to those having scss or sass
   extension. Ignores files matching any ignored regexps."
  [dir ignored-files]
  (let [^java.io.FilenameFilter filename-filter (cryogen-io/match-re-filter #"(?i:s[ca]ss$)")]
    (->> (.listFiles (io/file "." dir) filename-filter)
         (filter #(not (.isDirectory ^java.io.File %)))
         (filter (cryogen-io/ignore ignored-files))
         (map #(.getName ^java.io.File %)))))

(defn compile-sass-dir!
  "Given a sass directory (or file), output the resulting CSS in the
   same dir. All error handling is done by sh / launching the sass
   command."
  [{:keys [sass-dir sass-path compass-path]}]
  (shell/with-sh-dir
    "."
    (let [sass-argument (str sass-dir ":" sass-dir)]
      (if (compass-installed? compass-path)
        (sh sass-path "--compass" "--update" sass-argument)
        (sh sass-path "--update" sass-argument)))))

(defn compile-sass->css!
  "Given a directory or directories in sass-src, looks for all Sass files and compiles them.
   Prompts you to install sass if it finds Sass files but can't find the command. Shows you
   any problems it comes across when compiling. "
  [{:keys [sass-src theme-sass-src sass-path ignored-files] :as opts}]
  (if (and (not (empty? (concat sass-src theme-sass-src)))
           (not (sass-installed? sass-path)))
    (println
      (red (str "Sass seems not to be installed, but you have scss / sass files in "
                sass-src
                " - You might want to install it here: sass-lang.com")))
    (doseq [sass-dir (concat
                      (map (partial cryogen-io/path "content") sass-src)
                      (map (partial cryogen-io/path "themes" (:theme opts)) theme-sass-src))]
      (when (seq (find-sass-files sass-dir ignored-files))
        (println "\t" (cyan sass-dir) "-->" (cyan sass-dir))
        (let [result (compile-sass-dir! (assoc opts :sass-dir sass-dir))]
          (if-not (zero? (:exit result))
            (println (red (:err result))
                     (red (:out result)))))))))
