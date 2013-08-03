(ns espresso-art.core
  (:use
    [twitter.oauth]
    [twitter.callbacks]
    [twitter.callbacks.handlers]
    [twitter.api.restful]
    [espresso-art.config])
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string])
  (:import
    (twitter.callbacks.protocols SyncSingleCallback))
  (:gen-class))

(def espresso-text "espresso")
(def file-name "latest-tweet-id-read")
(def latest-tweet-id-read (atom 0))

(defn get-home-timeline [from-tweet-id]
  (println "Getting home timeline...")
  (:body (statuses-home-timeline :oauth-creds my-creds)))

(defn should-retweet? [tweet-json]
  (seq (some->> tweet-json (:entities)
                           (:hashtags)
                           (filter #(= espresso-text (:text %))))))

(defn retweet! [tweet-json]
  (do
    (println (str "Retweeting: " (:text tweet-json)))
    (try
      (statuses-retweet-id :oauth-creds my-creds :params {:id (:id tweet-json)})
      (catch Exception e
        (println (str "Could not reweet " (:text tweet-json) ": " (.getMessage e)))))))

(defn loop-iter []
  (let [timeline          (get-home-timeline @latest-tweet-id-read)
        latest            (apply max (map :id timeline))
        tweets-to-retweet (filter should-retweet? timeline)
        _                 (println "42")]
    (println latest)
    (doall (map retweet! tweets-to-retweet))
    (reset! latest-tweet-id-read latest)
    (spit file-name (str @latest-tweet-id-read))))

(defn -main
  "Retweets all tweets from people it's following that mention #espresso"
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (println "Starting batch retweeter!")
  (reset! latest-tweet-id-read (-> file-name slurp string/trim Long.))
  (while true
    (loop-iter)
    (println "Sleeping for 2 minutes...")
    (Thread/sleep 120000)))
