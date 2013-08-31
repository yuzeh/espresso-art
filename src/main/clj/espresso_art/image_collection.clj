(ns espresso-art.image-collection
  (:use [espresso-art.twitter]
        [seesaw.core]
        [seesaw.mig])
  (:require [clojure.core.async :as async]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]
            [clj-http.client :as http]
            [espresso-art.fs :as fs])
  (:gen-class))

(def current-image (atom nil))

(defn start-enqueue-tweet-thread! [twitter-user image-url-buffer]
  (async/thread
    (loop [previous-id nil
           since-id    nil]
      (if (or (nil? since-id) (not= since-id previous-id))
        (let [timeline          (get-user-timeline twitter-user since-id)
              next-id           (min-id timeline)
              tweets-with-photo (filter contains-photo? timeline)]
          (doseq [tweet tweets-with-photo]
            (log/debugf "Getting photo for URL: %s" (get-photo-url tweet))
            (async/>!! image-url-buffer (get-photo-url tweet)))
          (recur since-id next-id))
        (async/close! image-url-buffer)))))

(def espresso-button-group (button-group))

(defn save-image [image-url local-path]
  (with-open [stream (io/output-stream local-path)]
    (.write stream (-> image-url
                       (http/get {:as :byte-array})
                       :body))))

(defn display-image!
  [next-image widget]
  (log/info next-image)
  (reset! current-image next-image)
  (config! widget :icon (icon next-image)))

(defn mk-espresso-button
  [component-id is-espresso? app-config]
  (let [button (button
                 :id     component-id
                 :halign :center
                 :group  espresso-button-group
                 :class  :espresso-button
                 :text   (if is-espresso? "Espresso!" "Not espresso!"))]
    (listen button
      :mouse-clicked (fn [e]
                       (let [{:keys [data-directory url->file-name image-url-buffer]} app-config
                             cur-image @current-image
                             full-path (fs/join
                                         data-directory
                                         (str
                                           (if is-espresso? "1" "0")
                                           "-"
                                           (url->file-name cur-image)))]
                         (save-image cur-image full-path)
                         (if-let [next-image (async/<!! image-url-buffer)]
                           (display-image! next-image (select (to-root e) [:#image-panel]))
                           (config! (select (to-root e) [:.espresso-button]) :enabled? false)))))
    button))

(defn mk-frame [app-config]
  (let [is-espresso-button  (mk-espresso-button :is-espresso true app-config)
        not-espresso-button (mk-espresso-button :not-espresso false app-config)
        image-panel         (label :id :image-panel :icon (icon nil))
        button-panel        (mig-panel
                              :constraints ["" "[][]" ""]
                              :items       [[is-espresso-button  ""]
                                            [not-espresso-button ""]])
        content-window      (mig-panel
                              :constraints ["debug" "[center]" ""]
                              :items       [[image-panel  "center wrap"]
                                            [button-panel "center"]])]
    (frame
      :title        "Espresso Art Image Collector"
      :content      content-window
      :minimum-size [480 :by 640]
      :on-close     :exit)))

(defn scrape-images-for-twitter-user
  "For a given user, starts a loop to scrape all of the user's photos. Puts the url information
   into a queue for the UI thread to consume."
  [twitter-user]
  (let [image-url-buffer (async/chan 100)
        app-config       {:data-directory (format "data/twitter-user/%s/" twitter-user)
                          :url->file-name  fs/file-name
                          :image-url-buffer image-url-buffer}
        frame            (mk-frame app-config)]
    (native!)
    (fs/mkdirs (:data-directory app-config))
    (start-enqueue-tweet-thread! twitter-user image-url-buffer)
    (invoke-later
      (-> frame
          pack!
          show!)
    (display-image! (async/<!! image-url-buffer) (select frame [:#image-panel])))))

(defn -main [& args]
  (let [[options arguments banner]
        (cli/cli args
                 ;; ... should I file a bug?
                 ;;"Runs the image collector, which collects images from different sources."
                 ["--twitter-user" "Twitter user to scrape photos from. First priority."])
        {:keys [twitter-user]} options]
    (log/info "Starting image collector!")
    (cond
      twitter-user (scrape-images-for-twitter-user twitter-user)
      :else        (log/error "Misconfiguration - could not use scraper!"))))
