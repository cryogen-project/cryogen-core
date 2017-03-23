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
   output the resulting css in the same dir. All error handling is
   done by sh / launching the sass command."
  [{:keys [sass-dir sass-path compass-path base-dir]}]
  (shell/with-sh-dir base-dir
    (if (compass-installed? compass-path)
      (sh sass-path "--compass" "--update" sass-dir)
      (sh sass-path "--update" sass-dir))))

(defn compile-sass->css!
  "Given a directory(s) sass-src, looks for all sass files and compiles them.
   Prompts you to install sass if it finds sass files but can't find
   the command. Shows you any problems it comes across when compiling. "
  [{:keys [sass-src sass-path ignored-files base-dir] :as opts}]
  (if (not (coll? sass-src))
    (recur (assoc opts :sass-src [sass-src]))
    (doseq [sass-dir sass-src]
      (when (seq (find-sass-files base-dir sass-dir ignored-files))
        (if (sass-installed? sass-path)
          (do
            (println "\t" (cyan sass-dir) "-->" (cyan sass-dir))
            (let [result (compile-sass-file! (assoc opts :sass-dir sass-dir))]
              (if (zero? (:exit result))
                (println "Successfully compiled sass files")
                (println (red (:err result))
                         (red (:out result))))))
          (println "Sass seems not to be installed, but you have scss / sass files in "
                   sass-dir
                   " - You might want to install it here: sass-lang.com"))))))
