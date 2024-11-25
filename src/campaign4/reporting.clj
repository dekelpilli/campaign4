(ns campaign4.reporting
  (:require
    [campaign4.paths :as paths]
    [campaign4.util :as u]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [hato.client :as hato]
    [jsonista.core :as j]
    [methodical.core :as m]
    [puget.printer :as pp])
  (:import
    (java.util.concurrent Executors)
    (name.fraser.neil.plaintext diff_match_patch$Operation)))

(def ^:private discord-username "\uD83D\uDCB0 Placeholder DMs \uD83D\uDCB0")

(def ^:private key-priority (->> [:name :base-type :mods :effects :formatted :effect :tags]
                                 (map-indexed (fn [i k] [k i]))
                                 (into {})))
(defn- priority-comparator [a b]
  (compare (key-priority a (abs (hash a)))
           (key-priority b (abs (hash b)))))

(defn- derive-type [loot]
  (cond
    (:id loot) :loot
    (-> loot meta ::type) (-> loot meta ::type name keyword)
    (string? loot) :string
    (sequential? loot) :sequential
    (:character loot) :helmet
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
(m/defmulti format-loot-result :id)

(m/defmethod format-loot-result :unique [{:keys [result]}]
  (mapv format-loot result))

(m/defmethod format-loot-result :ring [{:keys [result]}]
  (mapv format-loot result))

(m/defmethod format-loot-result :curio [{:keys [result]}]
  (-> (with-meta result {::type :curios})
      format-loot))

(m/defmethod format-loot-result :omen [{:keys [result]}]
  {:title "Reroll and gain omen"
   :body  result})

(m/defmethod format-loot-result :default [{:keys [result]}]
  (format-loot result))

(defn- format-coll [coll]
  (->> (mapv #(str "- " %) coll)
       (str/join \newline)))

(m/defmethod format-loot :sequential [coll]
  {:body (format-coll coll)})

(defn- ansi-colour [s colour]
  (let [colour (case colour
                 :red 31
                 :green 32)]
    (str "\u001b[0;" colour ";40m" s "\u001b[0m")))

(defn- unique-changelog [mods]
  (keep
    (fn [{:keys [effect change]}]
      (cond
        (nil? change) nil
        (= :new change) (str "- " (ansi-colour effect :green))
        (= :removed change) (str "- " (ansi-colour effect :red))
        (:diff change) (doto (->> (mapv
                                    (fn [diff]
                                      (cond
                                        (= diff_match_patch$Operation/EQUAL (.-operation diff)) (.-text diff)
                                        (= diff_match_patch$Operation/INSERT (.-operation diff)) (ansi-colour (.-text diff) :green)
                                        (= diff_match_patch$Operation/DELETE (.-operation diff)) (ansi-colour (.-text diff) :red)))
                                    (:diff change))
                                  str/join
                                  (str "- "))
                         println)))
    mods))

(m/defmethod format-loot :unique [{:keys [name base level mods]}]
  (let [item (->> (remove (comp #{:removed} :change) mods)
                  (mapv :effect)
                  format-coll)
        changelog (when (> level 1)
                    (unique-changelog mods))]
    {:title (format "%s (level %s unique; %s)" name level base)
     :body  (cond-> item
                    changelog (str "\n## Changelog:\n```ansi\n" (str/join \newline changelog) "```"))}))

(m/defmethod format-loot :relic [{:keys [name base level mods]}]
  {:title (format "%s (level %s relic; %s)" name level base)
   :body  (format-coll (mapv :formatted mods))})

(defn- format-vial [{:keys [name character item]}]
  {:title (format "%s (vial)" name)
   :body  (format "### Effects when drunk:\n%s\n### Effects when applied to item:\n%s"
                  character item)})

(m/defmethod format-loot :vial [loot]
  (format-vial loot))

(m/defmethod format-loot :talisman [{:strs [above below unconditional]}]
  {:title "Talisman"
   :body  (format-coll (mapv :formatted [above below unconditional]))})

(m/defmethod format-loot :gem [{:keys                 [name type cr]
                                {trait-name :name
                                 entries    :entries} :trait}]
  {:title (format "Gem (CR%s; %s; %s) - %s"
                  (long cr) type name trait-name)
   :body  (->> (format-coll entries)
               (format "```%s```"))})

(m/defmethod format-loot :crafting [{:keys [character item name effect] :as loot}]
  (if (and character item)
    (format-vial loot)
    {:title (str name (if (str/includes? name "Shrine")
                        ""
                        " (crafting consumable)"))
     :body  effect}))

(m/defmethod format-loot :curios [curios]
  {:title "Curios"
   :body  (->> (sort-by (juxt #(str/starts-with? % "negated-")
                              identity) curios)
               (mapv (fn [s]
                       (if (str/starts-with? s "negated-")
                         (->> (subs s 8)
                              str/capitalize
                              (str ":x: "))
                         (->> (str/capitalize s)
                              (str ":white_check_mark: ")))))
               format-coll)})

(m/defmethod format-loot :tarot [cards]
  (mapv
    (fn [{:keys [name effect]}]
      {:title (str name " (tarot card)")
       :body  effect})
    cards))

(m/defmethod format-loot :helmet [{:keys [character mods]}]
  {:title (format "Helmet (for %s)" character)
   :body  (format-coll (mapv :formatted mods))})

(defn- loot-title [title id]
  (or title
      (-> id
          name
          (str/split #"-")
          (->> (mapv str/capitalize)
               (str/join " ")))))

(m/defmethod format-loot :loot [{:keys [omen id n] :as loot}]
  (let [formatted (format-loot-result loot)
        formatted (if (vector? formatted) formatted [formatted])
        formatted-results (update formatted 0
                                  (fn [formatted] (-> (assoc formatted :roll n)
                                                      (update :title loot-title id))))]
    (cond-> formatted-results
            omen (conj {:title "Omen"
                        :body  omen}))))

(m/defmethod format-loot :enchanted [{:keys [base enchants]}]
  {:title (format "Enchanted %s (receptacle)" base)
   :body  (-> (mapv :formatted enchants)
              format-coll)})

(m/defmethod format-loot :divinity [{:keys [tier modifier]}]
  {:title (str (some (fn [{:keys [name levels]}]
                       (when (= (nth levels (dec tier))
                                modifier)
                         name))
                     (vals paths/divinity-paths))
               " (" tier ")")
   :body  (:effect modifier)})

(m/defmethod format-loot :ring [{:keys [name points formatted]}]
  {:title (format "%s (%s point ring)" name points)
   :body  formatted})

(m/defmethod format-loot :default [loot]
  (when loot
    {:body (str loot)}))

(defn format-loot-message [v]
  (->> (cond-> v (map? v) vector)
       (mapv (fn [{:keys [roll title body]}]
               (str (when roll (format "# Roll: %s\n" roll))
                    (when title (str "## " title \newline))
                    body)))
       (str/join \newline)))

(defn- format-and-report! [loot]
  (let [{:keys [channel_id id]} (-> (hato/request {:method       :post
                                                   :url          (:detailed-loot-webhook u/config)
                                                   :query-params {:wait true}
                                                   :content-type :application/json
                                                   :body         (j/write-value-as-string
                                                                   {:content    (str "```edn\n"
                                                                                     (pp/pprint-str
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
                                    u/parse-json)]
    (hato/request {:method       :post
                   :url          (:loot-webhook u/config)
                   :content-type :application/json
                   :body         (j/write-value-as-string
                                   {:content    (-> (format-loot loot)
                                                    format-loot-message)
                                    :embeds     [{:title "Full details"
                                                  :type  "link"
                                                  :url   (format "https://discord.com/channels/%s/%s/%s"
                                                                 (:discord-server-id u/config)
                                                                 channel_id
                                                                 id)}]
                                    :avatar_url (:discord-avatar u/config)
                                    :username   discord-username})})))

(def executor (Executors/newSingleThreadExecutor))

(defn report-loot! [loot]
  (when loot
    (.submit executor ^Runnable #(format-and-report! loot))))

(comment
  (-> {:result [{:name "ancient" :effect "reroll unique"}
                (-> (campaign4.uniques/new-unique)
                    (campaign4.uniques/at-level 1))]
       :id     :unique
       :n      20}
      format-loot)
  (-> {:id     :gold
       :result "20 gold"
       :n      6}
      format-loot
      format-loot-message)
  (-> (campaign4.crafting/loot-result)
      report-loot!)
  (-> {:curios (-> (repeatedly 4 campaign4.curios/new-curio)
                   vec)}
      report-loot!))
