(defproject espresso-art "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"project"             "file:repo"
                 "sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :source-paths ["src/main/clj"]
  :dependencies [;; Clojure libraries
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.0-SNAPSHOT"]
                 [org.clojure/tools.cli "0.2.4"]
                 [org.clojure/tools.logging "0.2.6"]

                 ;; Utility libraries
                 [clj-http "0.7.6"]
                 [log4j/log4j "1.2.17"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [seesaw "1.4.3"]

                 ;; Twitter
                 [twitter-api "0.7.4"]

                 ;; OpenCV
                 [local/opencv "2.4.5"]
                 [local/opencv-native "2.4.5"]]
  :aot [espresso-art.server espresso-art.image-collection])
