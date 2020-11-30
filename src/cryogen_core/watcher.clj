(ns cryogen-core.watcher
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [hawk.core :as hawk]
            [pandect.algo.md5 :as md5]
            [cryogen-core.io :as cryogen-io]))

(defn get-assets [path ignored-files]
  (->> path
       io/file
       file-seq
       (filter #(not (.isDirectory ^java.io.File %)))
       (filter (cryogen-io/ignore ignored-files))))

(defn checksums [path ignored-files]
  (let [files (get-assets path ignored-files)]
    (zipmap (map md5/md5 files) files)))

(defn find-changes [old-sums new-sums]
  (let [old-sum-set (-> old-sums keys set)
        new-sum-set (-> new-sums keys set)]
    (when-some [changes (set/difference new-sum-set old-sum-set)]
      (vals (select-keys new-sums changes)))))

(defn watch-assets [sums root ignored-files callback]
  (let [new-sums (checksums root ignored-files)]
    (when-let [changeset (find-changes @sums new-sums)]
      (callback changeset)
      (reset! sums new-sums))))

(defn watch-with-fallback!
  "Wraps hawk/watch! to swap in a polling implementation if any of the native
   OS file events interfaces fail."
  [opts & groups]
  (try
    (apply hawk/watch! opts groups)
    (catch Error e
      (prn e "WARN - no native fs events; falling back to polling filesystem")
      (apply hawk/watch! (assoc opts :watcher :polling) groups))))

(defn start-watcher-for-changes!
  "Start watching files under `root` for changes, excluding `ignored-files`
  (as per [[cryogen-core.io/ignore]]), and call `(callback <callback-args> changeset)`
  upon every change detected. Where:
  - `changeset` is a sequence of `java.io.File` with `root`-relative paths.
  - `callback-args` are any additional arguments to the callback, which will be
    passed before the changeset"
  [root ignored-files callback & callback-args]
  (let [sums (atom (checksums root ignored-files))
        handler (fn [ctx e]
                  (watch-assets sums root ignored-files #(apply callback (concat callback-args %&))))]
    (watch-with-fallback! {} [{:paths   [root]
                               :handler handler}])))

(defn start-watcher!
  "Same as [[start-watcher-for-changes!]] but expects 0-argument action"
  [root ignored-files action]
  (start-watcher-for-changes! root ignored-files (fn [_] (action))))
