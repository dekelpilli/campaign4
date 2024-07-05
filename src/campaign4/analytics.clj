(ns campaign4.analytics
  (:require
    [campaign4.db :as db]))

(def current-session (atom nil))

(defn record! [event amount]
  (when-let [current-session @current-session]
    (db/execute! {:insert-into   :analytics
                  :values        [{:type    event
                                   :session current-session
                                   :amount  amount}]
                  :on-conflict   [:type :session]
                  :do-update-set {:amount [:+ :EXCLUDED.amount amount]}})))

(defn set-session! [n]
  (reset! current-session n))
