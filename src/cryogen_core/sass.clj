(ns cryogen-core.sass
  (:require [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            [text-decoration.core :refer :all]
            [cryogen-core.io :refer [ignore match-re-filter]]))

(defmacro sh
  [& args]
  (let [valid-args (remove nil? args)]
    `(shell/sh ~@valid-args)))

(defn sass-installed?
  "Checks for the installation of Sass."
  []
  (= 0 (:exit (sh "sass" "--version"))))

(defn compass-installed?
   "Checks for the installation of Compass."
   []
   (= 0 (:exit (sh "compass" "--version"))))

(defn find-sass-files
  "Given a Diretory, gets files, Filtered to those having scss or sass
  extention. Ignores files matching any ignored regexps."
  [dir ignored-files]
  (let [filename-filter (match-re-filter #"(?i:s[ca]ss$)")]
    (->> (.listFiles (io/file dir) filename-filter)
         (filter #(not (.isDirectory %)))
         (filter (ignore ignored-files))
         (map #(.getName %)))))

(defn compile-sass-file!
  "Given a sass file which might be in src-sass directory,
  output the resulting css in dest-sass. All error handling is
    done by sh / launching the sass command."
  [sass-file
   src-sass
   dest-sass
   compass?]
  (sh "sass"
      "--update"
      (when compass? "--compass")
      (str src-sass "/" sass-file)
      (str dest-sass "/" )))

(defn compile-sass->css!
  "Given a directory src-sass, looks for all sass files and compiles them into
dest-sass. Prompts you to install sass if he finds sass files and can't find
the command. Shows you any problems it comes across when compiling. "
  [{:keys [src-sass
           dest-sass
           ignored-files]}]
  (let [sass-files (find-sass-files src-sass ignored-files)]
    (if (seq sass-files)
      ;; I found sass files,
      ;; If sass is installed
      (if (sass-installed?)
        (let [compass? (compass-installed?)]
         ;; I compile all files
         (doseq [a-file sass-files]
           (println "Compiling Sass File:" a-file src-sass dest-sass)
           (let [result   (compile-sass-file! a-file src-sass dest-sass compass?)]
             (if (zero? (:exit result))
               ;; no problems in sass compilation
               (println "Successfully compiled:" a-file)
               ;; else I show the error
               (println (red (:err result))
                        (red (:out result)))))))
        ;; Else I prompt to install Sass
        (println "Sass seems not to be installed, but you have scss / sass files in "
                 src-sass
                 " - You might want to install it here: sass-lang.com")))))
