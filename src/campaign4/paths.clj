(ns campaign4.paths
  (:require
    [campaign4.db :as db]
    [campaign4.util :as u]
    [clojure.string :as str]))

(def divinity-paths (->> (u/load-data :divinity-paths)
                         (u/assoc-by :name)))

(defn path-descriptions []
  (->> (vals divinity-paths)
       (mapv (fn [{:keys [name info]}]
               (format "%s: %s" name info)))))

(defn- fetch-incomplete-path [character]
  (-> (db/execute! {:select [:*]
                    :from   [:divinity-progress]
                    :where  [:and
                             [:= :character character]
                             [:< :progress 5]]})
      first))

(defn new-path-progress [character divinity-path]
  (when-let [character (when (u/characters character)
                         (name character))]
    (when-not (fetch-incomplete-path character)
      {:insert-into :divinity-progress
       :values      [{:progress  1
                      :path      (->> (str/split divinity-path #" ")
                                      (mapv str/capitalize)
                                      (str/join \space))
                      :character character}]})))

(defn progress-path [character]
  (when-let [{:keys [path progress]
              :as   current-path} (when (u/characters character)
                                    (fetch-incomplete-path (name character)))]
    (db/execute! {:insert-into   :divinity-progress
                  :values        [(update current-path :progress inc)]
                  :on-conflict   [:character :path]
                  :do-update-set {:progress :EXCLUDED.progress}})
    {:modifier (get-in divinity-paths [path :levels progress])
     :tier     (inc progress)}))
