(ns cryogen-core.watcher-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [cryogen-core.watcher :refer [start-watcher-for-changes! start-watcher! find-changes]])
  (:import [java.io File]
           [java.nio.file Files]))

(defn- temp-dir
  "Create a temporary directory and return its File."
  [prefix]
  (-> (Files/createTempDirectory prefix (into-array java.nio.file.attribute.FileAttribute []))
      .toFile))

(defn- delete-recursive!
  "Delete directory and all contents."
  [^File dir]
  (doseq [^File child (.listFiles dir)]
    (if (.isDirectory child)
      (delete-recursive! child)
      (.delete child)))
  (.delete dir))

(defn- await-change
  "Start watcher on `root`, run `actions` (fn [root]), wait up to `timeout-ms`
  for the callback to fire. Returns the changeset or ::timed-out."
  [timeout-ms ignored-files root actions]
  (let [p (promise)
        _ (start-watcher-for-changes! root ignored-files
                                      (fn [changeset]
                                        (deliver p changeset)))
        _ (Thread/sleep 500)  ;; give the watcher time to register + start polling
        _ (actions root)
        result (deref p timeout-ms ::timed-out)]
    result))

;; --- find-changes tests ---

(deftest test-find-changes-detects-new-file
  (testing "find-changes detects a new file (new MD5 not in old set)"
    (let [old-sums {"abc123" (io/file "/tmp/a.txt")}
          new-sums {"abc123" (io/file "/tmp/a.txt")
                    "def456" (io/file "/tmp/b.txt")}]
      (is (= [(io/file "/tmp/b.txt")] (find-changes old-sums new-sums))))))

(deftest test-find-changes-detects-modified-file
  (testing "find-changes detects modified file (different MD5 for same path)"
    (let [old-sums {"oldmd5" (io/file "/tmp/a.txt")}
          new-sums {"newmd5" (io/file "/tmp/a.txt")}]
      (is (= [(io/file "/tmp/a.txt")] (find-changes old-sums new-sums))))))

(deftest test-find-changes-returns-nil-when-unchanged
  (testing "find-changes returns nil when nothing changed"
    (let [sums {"abc" (io/file "/tmp/a.txt")
                "def" (io/file "/tmp/b.txt")}]
      (is (nil? (find-changes sums sums))))))

(deftest test-find-changes-returns-empty-when-only-deleted
  (testing "find-changes returns empty seq for deleted files (MD5 diff is new - old)"
    (let [old-sums {"abc" (io/file "/tmp/a.txt") "def" (io/file "/tmp/b.txt")}
          new-sums {"abc" (io/file "/tmp/a.txt")}]
      (is (empty? (find-changes old-sums new-sums))))))

;; --- integration tests ---

(deftest test-file-creation-detected
  (testing "Creating a new file triggers the callback"
    (let [dir (temp-dir "cryogen-create-")
          root (.getAbsolutePath dir)
          result (await-change 10000 #{}
                               root
                               (fn [root]
                                 (spit (io/file root "new-file.txt") "hello")))]
      (try
        (is (seq result) (str "Expected changeset, got: " result))
        (finally
          (delete-recursive! dir))))))

(deftest test-file-modification-detected
  (testing "Modifying an existing file triggers the callback"
    (let [dir (temp-dir "cryogen-modify-")
          root (.getAbsolutePath dir)
          f (io/file root "existing.txt")]
      (spit f "initial content")
      ;; Start watcher after creating the file so it's in the initial checksum
      (let [result (await-change 10000 #{}
                                 root
                                 (fn [_]
                                   (spit f "modified content")))]
        (try
          (is (seq result) (str "Expected changeset, got: " result))
          (finally
            (delete-recursive! dir)))))))

(deftest test-start-watcher-calls-action
  (testing "start-watcher! calls the 0-arg action"
    (let [dir (temp-dir "cryogen-action-")
          root (.getAbsolutePath dir)
          p (promise)
          _ (start-watcher! root #{} #(deliver p :called))
          _ (Thread/sleep 500)
          _ (spit (io/file root "trigger.txt") "content")
          result (deref p 10000 ::timed-out)]
      (try
        (is (= :called result))
        (finally
          (delete-recursive! dir))))))

(deftest test-new-subdirectory-detected
  (testing "Creating a new directory and writing a file inside triggers the callback"
    (let [dir (temp-dir "cryogen-subdir-")
          root (.getAbsolutePath dir)
          result (await-change 10000 #{}
                               root
                               (fn [_]
                                 (let [subdir (io/file root "newdir")]
                                   (.mkdir subdir)
                                   (Thread/sleep 500)  ;; wait for watcher to register new dir
                                   (spit (io/file subdir "nested.txt") "nested"))))]
      (try
        (is (seq result) (str "Expected changeset, got: " result))
        (finally
          (delete-recursive! dir))))))

(deftest test-recursive-directory-watching
  (testing "Files in subdirectories created before watcher starts are detected"
    (let [dir (temp-dir "cryogen-recursive-")
          root (.getAbsolutePath dir)
          subdir (io/file root "subdir")]
      (.mkdir subdir)
      (spit (io/file subdir "nested.txt") "initial")
      (let [result (await-change 10000 #{}
                                 root
                                 (fn [_]
                                   (spit (io/file subdir "nested.txt") "changed")))]
        (try
          (is (seq result) (str "Expected changeset, got: " result))
          (finally
            (delete-recursive! dir)))))))
