(ns campaign4.reporting
  (:require
    [campaign4.util :as u]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [hato.client :as hato]
    [jsonista.core :as j]
    [methodical.core :as m]))

(def ^:private discord-username "\uD83D\uDCB0 Placeholder DMs \uD83D\uDCB0")
(def ^:private pretty-mapper (j/object-mapper {:pretty true}))

(def ^:private key-priority (->> [:name :base-type :mods :effects :formatted :effect :tags]
                                 (map-indexed (fn [i k] [k i]))
                                 (into {})))
(defn- priority-comparator [a b]
  (compare (key-priority a (abs (hash a)))
           (key-priority b (abs (hash b)))))

(defn- derive-type [loot]
  (cond
    (string? loot) :string
    (sequential? loot) :sequential
    (some? (:synergy? loot)) :ring
    (some? (:sold loot)) :relic
    (and (:name loot)
         (:level loot)) :unique
    (:item loot) :vial
    (get loot "above") :talisman
    (:cr loot) :gem
    (and (:name loot)
         (:effect loot)
         (= 2 (count loot))) :crafting
    (:tier loot) :divinity))

(m/defmulti format-loot derive-type)

(m/defmethod format-loot :sequential [coll]
  (-> (reduce
        (fn [^StringBuilder sb v]
          (let [^String s (if (string? v)
                            v
                            (format-loot v))]
            (-> (.append sb "- ")
                (.append ^String s)
                (.append "\n"))))
        (StringBuilder.)
        coll)
      str))

(m/defmethod format-loot :unique [{:keys [name base-type level mods]}]
  (-> (format "## %s (%s; level %s unique)\n" name base-type level)
      (str (format-loot (mapv :effect mods)))))

(m/defmethod format-loot :relic [{:keys [name base-type level mods]}]
  (-> (format "## %s (%s; level %s relic)\n" name base-type level)
      (str (format-loot (mapv :formatted mods)))))

(m/defmethod format-loot :vial [{:keys [name character item]}]
  (format "## %s (vial)\n### Effects when drunk:\n%s\n### Effects when applied to item:\n%s"
          name
          character
          item))

(m/defmethod format-loot :talisman [{:strs [above below unconditional]}]
  (->> [above below unconditional]
       (mapv :formatted)
       format-loot
       (str "## Talisman\n")))

(m/defmethod format-loot :gem [{:keys                 [name type cr]
                                {trait-name :name
                                 entries    :entries} :trait}]
  (format "## Gem (CR%s; %s; %s) - %s\n```%s```"
          (long cr)
          type
          name
          trait-name
          (format-loot entries)))

(m/defmethod format-loot :crafting [{:keys [name effect]}]
  (format "## %s%s\n%s"
          name
          (if (str/includes? name "Shrine")
            ""
            " (crafting consumable)")
          effect))

(m/defmethod format-loot :divinity [loot]
  (str loot)) ;TODO

(m/defmethod format-loot :default [loot] (str loot))

(defn send! [loot]
  (hato/request {:method       :post
                 :url          (:loot-webhook u/config)
                 :async?       true
                 :content-type :application/json
                 :body         (j/write-value-as-string
                                 {:content    (format-loot loot)
                                  :avatar_url (:discord-avatar u/config)
                                  :username   discord-username})})
  (hato/request {:method       :post
                 :url          (:detailed-loot-webhook u/config)
                 :async?       true
                 :content-type :application/json
                 :body         (j/write-value-as-string
                                 {:content    (str "```json\n"
                                                   (j/write-value-as-string ;TODO decide if this is better than EDN pretty printing
                                                     (walk/prewalk
                                                       (fn [x] (if (map? x)
                                                                 (->> (dissoc x :template)
                                                                      (into (sorted-map-by priority-comparator)))
                                                                 x))
                                                       loot)
                                                     pretty-mapper)
                                                   (str "\n```"))
                                  :avatar_url (:discord-avatar u/config)
                                  :username   discord-username})}))

(comment
  (-> (campaign4.uniques/new-unique)
      (campaign4.uniques/at-level 1)
      send!)
  (-> (campaign4.vials/new-vial)
      format-loot
      println)
  (-> (campaign4.crafting/crafting-loot)
      send!))
