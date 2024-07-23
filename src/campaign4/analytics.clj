(ns campaign4.analytics
  (:require [campaign4.persistence :as p]
            [clojure.core.async :as a]))

(def current-session (atom nil))

(defn record! [event amount]
  (when-let [current-session @current-session]
    (a/go
      (or (p/update-data!
            ::p/analytics
            {:filter {:type    [event]
                      :session [current-session]}}
            (fn [data] (update data :amount + amount)))
          (p/insert-data!
            ::p/analytics
            [{:type    event
              :session current-session
              :amount  amount}])))))

(defn set-session! [n]
  (reset! current-session n))
