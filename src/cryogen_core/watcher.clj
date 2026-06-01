(ns cryogen-core.watcher
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [pandect.algo.md5 :as md5]
            [cryogen-core.io :as cryogen-io])
  (:import [java.nio.file FileSystems Files LinkOption Path StandardWatchEventKinds WatchEvent WatchKey WatchService]
           [java.io File]))

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

(defn- register-recursive!
  "Recursively register `dir` and all its subdirectories with the WatchService."
  [^WatchService ws ^Path dir]
  (when (.isDirectory (.toFile dir))
    (.register dir ws
               (into-array java.nio.file.WatchEvent$Kind
                           [StandardWatchEventKinds/ENTRY_CREATE
                            StandardWatchEventKinds/ENTRY_MODIFY
                            StandardWatchEventKinds/ENTRY_DELETE]))
    (doseq [^File child (.listFiles (.toFile dir))]
      (when (.isDirectory child)
        (register-recursive! ws (.toPath child))))))

(defn start-watcher-for-changes!
  "Start watching files under `root` for changes, excluding `ignored-files`
  (as per [[cryogen-core.io/ignore]]), and call `(callback <callback-args> changeset)`
  upon every change detected. Where:
  - `changeset` is a sequence of `java.io.File` with `root`-relative paths.
  - `callback-args` are any additional arguments to the callback, which will be
    passed before the changeset"
  [root ignored-files callback & callback-args]
  (let [sums (atom (checksums root ignored-files))
        ws (.newWatchService (FileSystems/getDefault))
        root-path (.toPath (io/file root))
        handler (fn [_ _]
                  (watch-assets sums root ignored-files #(apply callback (concat callback-args %&))))
        thread (Thread.
                (fn []
                  (try
                    (loop []
                      (let [key (.take ws)]
                        (doseq [^WatchEvent event (.pollEvents key)]
                          (when (= StandardWatchEventKinds/ENTRY_CREATE (.kind event))
                            (let [child (.resolve root-path (.context event))]
                              (when (.isDirectory (.toFile child))
                                (register-recursive! ws child)))))
                        (handler nil nil)
                        (.reset key)
                        (recur)))
                    (catch InterruptedException _)
                    (catch java.nio.file.ClosedWatchServiceException _))))]
    (.setDaemon thread true)
    (register-recursive! ws root-path)
    (.start thread)
    ws))

(defn start-watcher!
  "Same as [[start-watcher-for-changes!]] but expects 0-argument action"
  [root ignored-files action]
  (start-watcher-for-changes! root ignored-files (fn [_] (action))))
