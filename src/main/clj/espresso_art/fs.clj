(ns espresso-art.fs
  (:import [java.io File FilenameFilter]))

(defn join
  "Joins one or more paths together"
  ([a] a)
  ([a b] (.getPath (File. a b)))
  ([a b & more] (reduce join (join a b) more)))

(defn file-name
  "Gets the file name of a url or path string."
  [path]
  (.getName (File. path)))

(defn mkdirs
  "Makes directory, recursively."
  [path]
  (.mkdirs (File. path)))