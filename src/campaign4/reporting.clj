(ns campaign4.reporting
  (:require
    [campaign4.util :as u]
    [clojure.core.async :as a]
    [clj-yaml.core :as yaml]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [hato.client :as hato]
    [jsonista.core :as j]
    [methodical.core :as m]))

(def ^:private discord-username "\uD83D\uDCB0 Placeholder DMs \uD83D\uDCB0")

(def ^:private key-priority (->> [:name :base-type :mods :effects :formatted :effect :tags]
                                 (map-indexed (fn [i k] [k i]))
                                 (into {})))
(defn- priority-comparator [a b]
  (compare (key-priority a (abs (hash a)))
           (key-priority b (abs (hash b)))))

(defn- derive-type [loot]
  (cond
    ;(:loot-type loot) (:loot-type loot)
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
    (:curios loot) :curios
    (:tier loot) :divinity))

(m/defmulti format-loot derive-type)

(m/defmethod format-loot :sequential [coll]
  (->> (mapv #(->> (format-loot %)
                   (str "- ")) coll)
       (str/join \newline)))

(m/defmethod format-loot :unique [{:keys [name base-type level mods]}]
  (-> (format "## %s (level %s unique; %s)\n" name level base-type)
      (str (format-loot (mapv :effect mods)))))

(m/defmethod format-loot :relic [{:keys [name base-type level mods]}]
  (-> (format "## %s (level %s relicl; %s)\n" name level base-type)
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

(m/defmethod format-loot :curios [{curios :result}]
  (->> (sort-by (juxt #(str/starts-with? % "negated-")
                      identity) curios)
       (mapv (fn [s]
               (if (str/starts-with? s "negated-")
                 (->> (subs s 8)
                      str/capitalize
                      (str ":x: "))
                 (->> (str/capitalize s)
                      (str ":white_check_mark: ")))))
       format-loot
       (str "## Curios\n")))

(defn _format-loot-result [n {:keys [omen result]}] ;TODO(?)
  (str
    (if omen
      (str "## Omen\n" omen \newline)
      "")
    (if (vector? result)
      (->> (mapv format-loot result)
           (str/join \newline))
      (format-loot result))))

(m/defmethod format-loot :divinity [loot]
  (str loot)) ;TODO

(m/defmethod format-loot :default [loot] (str loot))

(defn report-loot! [loot]
  (a/go
    (let [{:keys [channel_id id]} (-> (hato/request {:method       :post
                                                     :url          (:detailed-loot-webhook u/config)
                                                     :query-params {:wait true}
                                                     :as           :json
                                                     :content-type :application/json
                                                     :body         (j/write-value-as-string
                                                                     {:content    (str "```yaml\n"
                                                                                       (yaml/generate-string
                                                                                         (walk/prewalk
                                                                                           (fn [x] (if (map? x)
                                                                                                     (->> (dissoc x :template)
                                                                                                          (into (sorted-map-by priority-comparator)))
                                                                                                     x))
                                                                                           loot))
                                                                                       (str "\n```"))
                                                                      :avatar_url (:discord-avatar u/config)
                                                                      :username   discord-username})})
                                      :body
                                      (j/read-value j/keyword-keys-object-mapper))]
      (hato/request {:method       :post
                     :url          (:loot-webhook u/config)
                     :content-type :application/json
                     :body         (j/write-value-as-string
                                     {:content    (format-loot loot)
                                      :embeds     [{:title "Full details"
                                                    :type  "link"
                                                    :url   (format "https://discord.com/channels/%s/%s/%s"
                                                                   (:discord-server-id u/config)
                                                                   channel_id
                                                                   id)}]
                                      :avatar_url (:discord-avatar u/config)
                                      :username   discord-username})}))))

(comment
  (-> (campaign4.uniques/new-unique)
      (campaign4.uniques/at-level 1)
      report-loot!)
  (-> (campaign4.vials/new-vial)
      format-loot
      println)
  (-> (campaign4.crafting/crafting-loot)
      report-loot!)
  (-> {:curios (-> (repeatedly 4 campaign4.curios/new-curio)
                   vec)}
      report-loot!))
