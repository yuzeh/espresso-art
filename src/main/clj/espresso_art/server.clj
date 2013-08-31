(ns espresso-art.server
  (:require [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]
            [espresso-art.twitter :as twitter])
  (:gen-class))

(def file-name "latest-tweet-id-read")
(def latest-tweet-id-read (atom 0))

(defn should-retweet? [tweet-json]
  (every? identity ((juxt
                      twitter/contains-hashtag-espresso?
                      twitter/contains-photo?) tweet-json)))

(defn loop-iter []
  (when-let [timeline (seq (twitter/get-home-timeline @latest-tweet-id-read))]
    (let [latest            (twitter/max-id timeline)
          tweets-to-retweet (filter should-retweet? timeline)]
      (log/info "Reading tweets from" @latest-tweet-id-read "to" latest)
      (doall (map twitter/retweet! tweets-to-retweet))
      (reset! latest-tweet-id-read latest)
      (spit file-name (str @latest-tweet-id-read)))))

(defn -main [& args]
  (let [[options arguments banner]
        (cli/cli args
                 "Runs the server that powers @espresso_art's retweeting mechanisms."
                 ["-w" "--wait-time" "Time to wait between batches, in seconds"
                    :default 120 :parse-fn #(Integer. %)])
        wait-time (:wait-time options)]
    (log/info "Starting batch retweeter!")
    (reset! latest-tweet-id-read (-> file-name slurp string/trim Long.))
    (log/info "Starting with last read id:" @latest-tweet-id-read)
    (while true
      (loop-iter)
      (log/infof "Sleeping for %d seconds..." wait-time)
      (Thread/sleep (* wait-time 1000)))))
