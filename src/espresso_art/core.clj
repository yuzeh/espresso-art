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
  (let [base-params {:count 200}
        full-params (if (pos? from-tweet-id)
                      (assoc base-params :since-id @latest-tweet-id-read)
                      base-params)]
    (:body (statuses-home-timeline :oauth-creds my-creds :params full-params))))

(defn contains-hashtag-espresso? [tweet-json]
  (seq (some->> tweet-json
         (:entities)
         (:hashtags)
         (filter #(= espresso-text (:text %))))))

(defn contains-photo? [tweet-json]
  (seq (some->> tweet-json
         (:entities)
         (:media)
         (filter #(= "photo" (:type %))))))

(defn should-retweet? [tweet-json]
  (every? identity ((juxt
                      contains-hashtag-espresso?
                      contains-photo?) tweet-json)))

(defn retweet! [tweet-json]
  (do
    (println (str "Retweeting: " (:text tweet-json)))
    (try
      (statuses-retweet-id :oauth-creds my-creds :params {:id (:id tweet-json)})
      (catch Exception e
        (println (str "Could not reweet " (:text tweet-json) ": " (.getMessage e)))))))

(defn loop-iter []
  (when-let [timeline (seq (get-home-timeline @latest-tweet-id-read))]
    (let [latest            (apply max (map :id timeline))
          tweets-to-retweet (filter should-retweet? timeline)]
      (println (str "Reading tweets from " @latest-tweet-id-read " to " latest))
      (doall (map retweet! tweets-to-retweet))
      (reset! latest-tweet-id-read latest)
      (spit file-name (str @latest-tweet-id-read)))))

(defn -main
  "Retweets all tweets from people it's following that mention #espresso"
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (println "Starting batch retweeter!")
  (reset! latest-tweet-id-read (-> file-name slurp string/trim Long.))
  (println (str "Starting with last read id: " @latest-tweet-id-read))
  (while true
    (loop-iter)
    (println "Sleeping for 2 minutes...")
    (Thread/sleep 120000)))
