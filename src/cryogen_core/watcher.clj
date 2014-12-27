(ns cryogen-core.watcher
  (:require [clojure.java.io :refer [file]]
            [cryogen-core.io :refer [ignore]]))

(defn get-assets [root ignored-files]
  (->> root
       file
       file-seq
       (filter #(not (.isDirectory %)))
       (filter (ignore ignored-files))))

(defn sum-times [path ignored-files]
  (->> (get-assets path ignored-files) (map #(.lastModified %)) (reduce +)))

(defn watch-assets [root ignored-files action]
  (loop [times (sum-times root ignored-files)]
    (Thread/sleep 300)
    (let [new-times (sum-times root ignored-files)]
      (when-not (= times new-times)
        (action))
      (recur new-times))))

(defn start-watcher! [root ignored-files action]
  (doto (Thread. #(watch-assets root ignored-files action))
    (.setDaemon true)
    (.start)))
