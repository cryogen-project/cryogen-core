(ns cryogen-core.watcher
  (:require [clojure.java.io :refer [file]]
            [cryogen-core.io :refer [ignore]]
            [pandect.algo.md5 :refer [md5]]
            [hawk.core :as hawk]
            [clojure.set :as set]))

(defn get-assets [path ignored-files]
  (->> path
       file
       file-seq
       (filter #(not (.isDirectory ^java.io.File %)))
       (filter (ignore ignored-files))))

(defn checksums [path ignored-files]
  (let [files (get-assets path ignored-files)]
    (zipmap (map md5 files) files)))

(defn find-changes [old-sums new-sums]
  (let [old-sum-set (-> old-sums keys set)
        new-sum-set (-> new-sums keys set)]
    (when-some [changes (set/difference new-sum-set old-sum-set)]
      (vals (select-keys new-sums changes)))))

(defn watch-assets [sums root ignored-files action]
  (let [new-sums (checksums root ignored-files)]
    (when (find-changes @sums new-sums)
      (action)
      (reset! sums new-sums))))

(defn start-watcher! [root ignored-files action]
  (let [sums (atom (checksums root ignored-files))
        handler (fn [ctx e]
                  (watch-assets sums root ignored-files action))]
    (hawk/watch! [{:paths  [root]
                   :handler handler}])))
