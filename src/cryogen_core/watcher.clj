(ns cryogen-core.watcher
  (:require [clojure.java.io :refer [file]]
            [cryogen-core.io :refer [ignore]]
            [pandect.core :refer [md5]]
            [clojure.set :as set]))

(defn get-assets [path ignored-files]
  (->> path
       file
       file-seq
       (filter #(not (.isDirectory %)))
       (filter (ignore ignored-files))))

(defn checksums [path ignored-files]
  (let [files (get-assets path ignored-files)]
    (zipmap (map md5 files) files)))

(defn find-changes [old-sums new-sums]
  (let [old-sum-set (-> old-sums keys set)
        new-sum-set (-> new-sums keys set)]
    (when-some [changes (set/difference new-sum-set old-sum-set)]
      (vals (select-keys new-sums changes)))))

(defn watch-assets [root ignored-files action]
  (loop [sums (checksums root ignored-files)]
    (Thread/sleep 300)
    (let [new-sums (checksums root ignored-files)]
      (when (find-changes sums new-sums)
        (action))
      (recur new-sums))))

(defn start-watcher! [root ignored-files action]
  (doto (Thread. #(watch-assets root ignored-files action))
    (.setDaemon true)
    (.start)))
