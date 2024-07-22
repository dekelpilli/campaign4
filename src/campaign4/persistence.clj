(ns campaign4.persistence
  (:require
    [campaign4.util :as u]
    [clojure.string :as str]
    [hato.client :as hato]
    [jsonista.core :as j]))

;https://support.getgrist.com/api/#tag/records

(def grist-config (:grist u/config))

(defn grist-request [{:keys [path body] :as req}]
  (-> (assoc req
        :url (str "https://docs.getgrist.com/api" path)
        :oauth-token (:key grist-config))
      (cond-> body
              (assoc :body (j/write-value-as-string body)
                     :content-type :application/json))
      hato/request
      (update :body j/read-value j/keyword-keys-object-mapper)))

(def models
  {::monsters [[:name :text] ;TODO probably use malli here? some other DSL?
               [:type :text]
               [:book :text]
               [:cr :int]
               [:traits :json]]
   ::relics   [[:name :text]
               [:sold :boolean]
               [:base :text]
               [:level :int]
               [:starting :json]
               [:pool :json]
               [:levels :json]]})

(defn- records-path [table]
  (str "/docs/" (:document-id grist-config)
       "/tables/"
       (str/capitalize (name table))
       "/records"))

(defn insert-data! [table records]
  (grist-request
    {:method :post
     :body   {:records (mapv (fn [record] {:fields record}) records)}
     :path   (records-path table)}))

(defn query-data [table {:keys [filter] :as query}]
  (->> (grist-request
         {:method       :get
          :query-params (cond-> query
                                filter (assoc :filter (j/write-value-as-string filter)))
          :path         (records-path table)})
       :body
       :records
       (mapv (fn [{:keys [fields id]}]
               (with-meta fields {:record-id id})))))

(defn update-data! [table query update-fn]
  (when-let [new-data (->> (query-data table query)
                           (into [] (keep (fn [data]
                                            (some-> (update-fn data)
                                                    (with-meta (meta data))))))
                           not-empty)]
    (grist-request
      {:method :patch
       :body   {:records (mapv (fn [data]
                                 {:fields data
                                  :id     (-> data meta :record-id)})
                               new-data)}
       :path   (records-path table)})))

(comment
  (grist-request
    {:method :get
     :path   "/orgs/67020/workspaces"})

  (query-data ::monsters {:filter {:name ["Allosaurus"]}
                          :limit  1}))
