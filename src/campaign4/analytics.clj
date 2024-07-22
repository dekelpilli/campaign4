(ns campaign4.analytics)

(def current-session (atom nil))

(defn record! [event amount]
  (when-let [current-session @current-session]
    ;TODO upsert with persistence ns
    #_(db/execute! {:insert-into   :analytics
                  :values        [{:type    event
                                   :session current-session
                                   :amount  amount}]
                  :on-conflict   [:type :session]
                  :do-update-set {:amount [:+ :EXCLUDED.amount amount]}})))

(defn set-session! [n]
  (reset! current-session n))
