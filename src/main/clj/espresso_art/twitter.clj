(ns espresso-art.twitter
  (:use [twitter.oauth]
        [twitter.callbacks]
        [twitter.callbacks.handlers]
        [twitter.api.restful]
        [espresso-art.config])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]))

(defn contains-hashtag-espresso?
  "Returns truthy if tweet JSON contains #espresso, nil otherwise."
  [tweet]
  (seq (some->> tweet
         :entities
         :hashtags
         (filter #(= "espresso" (:text %))))))

(defn contains-photo?
  "Returns truthy if tweet JSON has a photo, nil otherwise."
  [tweet]
  (seq (some->> tweet
         :entities
         :media
         (filter #(= "photo" (:type %))))))

(defn get-photo-url [tweet]
  (-> tweet :entities :media first :media_url))

(defn retweet!
  "Retweets the given tweet, given the original tweet's JSON."
  [tweet]
  (do
    (log/infof "Retweeting [id: %d]: %s" (:id tweet) (:text tweet))
    (try
      (statuses-retweet-id :oauth-creds my-creds :params {:id (:id tweet)})
      (catch Exception e
        (log/warnf e "Could not retweet tweet with id %d" (:id tweet))))))

(defn get-home-timeline [from-tweet-id]
  (log/info "Getting home timeline...")
  (let [base-params {:count 200}
        full-params (if (pos? from-tweet-id)
                      (assoc base-params :since-id from-tweet-id)
                      base-params)]
    (try
      (:body (statuses-home-timeline :oauth-creds my-creds :params full-params))
      (catch Exception e
        (log/warnf e "Could not get home timeline!")
        nil))))

(defn get-user-timeline
  "Get the user timeline for a given user name."
  [user-name maximum-id]
  (let [base-params       {:screen-name user-name}
        full-params       (if maximum-id
                            (assoc base-params :max-id maximum-id)
                            base-params)
        timeline-response (statuses-user-timeline :oauth-creds my-creds :params full-params)]
    (:body timeline-response)))

(defn max-id
  "gets the maximum id in the timeline."
  [timeline]
  (apply max (map :id timeline)))

(defn min-id
  "gets the minimum id in the timeline."
  [timeline]
  (apply min (map :id timeline)))
