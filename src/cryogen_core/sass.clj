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
  "Given a Diretory, gets files, Filtered to those having scss or sass
   extention. Ignores files matching any ignored regexps."
  [base-dir dir ignored-files]
  (let [^java.io.FilenameFilter filename-filter (cryogen-io/match-re-filter #"(?i:s[ca]ss$)")]
    (->> (.listFiles (io/file base-dir dir) filename-filter)
         (filter #(not (.isDirectory ^java.io.File %)))
         (filter (cryogen-io/ignore ignored-files))
         (map #(.getName ^java.io.File %)))))

(defn compile-sass-file!
  "Given a sass file which might be in sass-src directory,
   output the resulting css in sass-dest. All error handling is
   done by sh / launching the sass command."
  [{:keys [sass-src sass-dest sass-path compass-path base-dir]}]
  (shell/with-sh-dir base-dir
    (if (compass-installed? compass-path)
      (sh sass-path "--compass" "--update" (str sass-src ":" sass-dest))
      (sh sass-path "--update" (str sass-src ":" sass-dest)))))

(defn compile-sass->css!
  "Given a directory sass-src, looks for all sass files and compiles them into
   sass-dest. Prompts you to install sass if he finds sass files and can't find
   the command. Shows you any problems it comes across when compiling. "
  [{:keys [sass-src sass-dest sass-path ignored-files base-dir] :as opts}]
  (when (seq (find-sass-files base-dir sass-src ignored-files))
    (if (sass-installed? sass-path)
      (do
        (println "\t" (cyan sass-src) "-->" (cyan sass-dest))
        (let [result (compile-sass-file! opts)]
          (if (zero? (:exit result))
            (println "Successfully compiled sass files")
            (println (red (:err result))
                     (red (:out result))))))
      (println "Sass seems not to be installed, but you have scss / sass files in "
               sass-src
               " - You might want to install it here: sass-lang.com"))))
