(ns campaign4.persistence
  (:require
    [campaign4.util :as u]
    [clojure.string :as str]
    [hato.client :as hato]
    [jsonista.core :as j]
    [malli.core :as m]
    [malli.transform :as mt]))

;https://support.getgrist.com/api/#tag/records

(def ^:private transformer (mt/string-transformer))
(def ^:private grist-config (:grist u/config))

(defn- grist-request [{:keys [path body] :as req}]
  (-> (assoc req
        :url (str "https://docs.getgrist.com/api" path)
        :oauth-token (:key grist-config))
      (cond-> body
              (assoc :body (j/write-value-as-string body)
                     :content-type :application/json))
      hato/request
      (update :body u/parse-json)))

(def ^:private models
  (let [json-type [:string {:encode/string j/write-value-as-string
                            :decode/string u/parse-json}]]
    {::monsters  [:map
                  [:name :string]
                  [:type :string]
                  [:book :string]
                  [:cr :int]
                  [:traits json-type]]
     ::relics    [:map
                  [:name :string]
                  [:sold :boolean]
                  [:base :string]
                  [:level :int]
                  [:starting json-type]
                  [:pool json-type]
                  [:levels json-type]]
     ::divinity  [:map
                  [:character :string]
                  [:path :string]
                  [:progress :int]]
     ::analytics [:map
                  [::type :string]
                  [:session :int]
                  [:amount :int]]}))

(defn- records-path [table]
  (str "/docs/" (:document-id grist-config)
       "/tables/"
       (str/capitalize (name table))
       "/records"))

(defn- encode [table data]
  (m/encode (table models) data transformer))

(defn- decode [table data]
  (m/decode (table models) data transformer))

(defn insert-data! [table data]
  (grist-request
    {:method :post
     :body   {:records (mapv (fn [record] {:fields (encode table record)}) data)}
     :path   (records-path table)}))

(defn query-data [table {:keys [filter] :as query}]
  (->> (grist-request
         {:method           :get
          :throw-exceptions false
          :query-params     (cond-> query
                                    filter (assoc :filter (j/write-value-as-string filter)))
          :path             (records-path table)})
       :body
       :records
       (mapv (fn [{:keys [fields id]}]
               (-> (decode table fields)
                   (with-meta {:record-id id}))))))

(defn update-data! [table query update-fn]
  (when-let [new-data (->> (query-data table query)
                           (into [] (keep (fn [data]
                                            (some-> (update-fn data)
                                                    (with-meta (meta data))))))
                           not-empty)]
    (grist-request
      {:method :patch
       :body   {:records (mapv (fn [data]
                                 {:fields (encode table data)
                                  :id     (-> data meta :record-id)})
                               new-data)}
       :path   (records-path table)})
    new-data))

(comment
  (grist-request
    {:method :get
     :path   "/orgs/67020/workspaces"})
  (m/decode
    (::monsters models)
    {:name "x"}
    ;{:registry models}
    (mt/string-transformer))
  (query-data ::monsters {:filter {:name ["Allosaurus"]}
                          :limit  1}))
