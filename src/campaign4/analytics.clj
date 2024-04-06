(ns campaign4.analytics
  (:require
    [campaign4.db :as db]))

(declare current-session)

(defn record! [event amount]
  (when (bound? #'current-session)
    (db/execute! {:insert-into   :analytics
                  :values        [{:type    event
                                   :session current-session
                                   :amount  amount}]
                  :on-conflict   [:type :session]
                  :do-update-set {:amount [:+ :EXCLUDED.amount amount]}})))

(defn set-session! [n]
  (alter-var-root #'current-session (constantly n)))
