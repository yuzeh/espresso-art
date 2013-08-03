(ns espresso-art.core
  (:use
    [twitter.oauth]
    [twitter.callbacks]
    [twitter.callbacks.handlers]
    [twitter.api.restful]
    [espresso-art.config])
  (:import
    (twitter.callbacks.protocols SyncSingleCallback))
  (:gen-class))

(def espresso-text "espresso")

(defn should-retweet? [tweet-json]
  (seq (some->> tweet-json (:entities)
                           (:hashtags)
                           (filter #(= espresso-text (:text %))))))

(defn retweet! [tweet-json]
  (statuses-retweet-id :oauth-creds my-creds
                       :params {:id (:id tweet-json)}))

(defn -main
  "Retweets all tweets from people it's following that mention #espresso"
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (println "Starting batch retweeter!")
  (let [home-timeline (statuses-home-timeline :oauth-creds my-creds)]
    (->> (:body home-timeline)
      (filter should-retweet?)
      (map (fn [tweet-json]
             (do
               (println (str "Retweeting: " (:text tweet-json)))
               (retweet! tweet-json)))))))
