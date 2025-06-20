(ns campaign4.reporting
  (:require
    [campaign4.paths :as paths]
    [campaign4.util :as u]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [hato.client :as hato]
    [jsonista.core :as j]
    [methodical.core :as m]
    [puget.printer :as pp]
    [randy.core :as r])
  (:import
    (java.io PushbackReader)
    (java.util.concurrent Executor Executors)
    (name.fraser.neil.plaintext diff_match_patch$Operation)))

(def ^:private words (-> (io/resource "words.edn")
                         io/reader
                         PushbackReader.
                         edn/read))

(def ^:private discord-username "\uD83D\uDCB0 Placeholder DMs \uD83D\uDCB0")
(def ^:private discord-content-size-limit 2000)

(def ^:private key-priority (->> [:name :base-type :mods :effects :formatted :effect :tags]
                                 (map-indexed (fn [i k] [k i]))
                                 (into {})))
(defn- priority-comparator [a b]
  (compare (key-priority a (abs (hash a)))
           (key-priority b (abs (hash b)))))

(defn- loot-message-unique-name []
  (->> (r/sample-without-replacement 2 words)
       (str/join \space)))

(defn- derive-type [loot]
  (cond
    (:id loot) :loot
    (-> loot meta ::type) (-> loot meta ::type name keyword)
    (string? loot) :string
    (sequential? loot) :sequential
    (:item loot) :vial
    (:character loot) :helmet
    (some? (:synergy? loot)) :ring
    (some? (:sold loot)) :relic
    (and (:name loot)
         (:level loot)) :unique
    (get loot "above") :talisman
    (:cr loot) :gem
    (and (:name loot)
         (:effect loot)
         (= 2 (count loot))) :crafting
    (:curios loot) :curios
    (:tier loot) :divinity
    (:enchants loot) :enchanted))

(m/defmulti format-loot derive-type)
(m/defmulti format-loot-result :id)

(m/defmethod format-loot-result :unique [{:keys [result]}]
  (mapv format-loot result))

(m/defmethod format-loot-result :ring [{:keys [result]}]
  (mapv format-loot result))

(m/defmethod format-loot-result :curio [{:keys [result]}]
  (-> (with-meta result {::type :curios})
      format-loot))

(m/defmethod format-loot-result :talisman [{:keys [result]}]
  (->> ((juxt :talisman :gem) result)
       (mapv format-loot)))

(m/defmethod format-loot-result :default [{:keys [result]}]
  (format-loot result))

(defn- format-coll [coll]
  (->> (mapv #(str "- " %) coll)
       (str/join \newline)))

(m/defmethod format-loot :sequential [coll]
  {:body (format-coll coll)})

(m/defmethod format-loot :enchanted [{:keys [enchants base]}]
  {:title (format "Enchanted item (%s)" base)
   :body  (format-coll (mapv :formatted enchants))})

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
        (:diff change) (->> (mapv
                              (fn [diff]
                                (cond
                                  (= diff_match_patch$Operation/EQUAL (.-operation diff)) (.-text diff)
                                  (= diff_match_patch$Operation/INSERT (.-operation diff)) (ansi-colour (.-text diff) :green)
                                  (= diff_match_patch$Operation/DELETE (.-operation diff)) (ansi-colour (.-text diff) :red)))
                              (:diff change))
                            str/join
                            (str "- "))))
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

(defn- format-title-kw [kw]
  (-> kw
      name
      (str/split #"-")
      (->> (mapv str/capitalize)
           (str/join " "))))

(defn- loot-title [title id]
  (or title
      (format-title-kw id)))

(defn- format-ticket [{:keys [ticket name]}]
  {:title (format "%s Ticket" (format-title-kw ticket))
   :body  (format "Can be redeemed at a carnival in any city to access a stand run by The %s." name)})

(m/defmethod format-loot :loot [{:keys [id n tickets] :as loot}]
  (let [formatted (format-loot-result loot)
        formatted (if (vector? formatted) formatted [formatted])]
    (-> (into formatted (map format-ticket) tickets)
        (update 0
                (fn [formatted] (-> (assoc formatted :roll n)
                                    (update :title loot-title id)))))))

(m/defmethod format-loot :enchanted [{:keys [base enchants]}]
  {:title (format "Enchanted %s (receptacle)" base)
   :body  (-> (mapv :formatted enchants)
              format-coll)})

(m/defmethod format-loot :divinity [{:keys [tier modifier spell]}]
  {:title (str (some (fn [{:keys [name levels]}]
                       (when (= (nth levels (dec tier))
                                modifier)
                         name))
                     (vals paths/divinity-paths))
               " (" tier ")")
   :body  (cond-> (:effect modifier)
                  spell (-> vector
                            (conj spell)
                            format-coll))})

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

(defn- detailed-loot-content [loot]
  (let [detailed-data (walk/prewalk
                        (fn [x] (if (map? x)
                                  (->> (dissoc x :template)
                                       (into (sorted-map-by priority-comparator)))
                                  x))
                        loot)
        content (str "```edn\n"
                     (pp/pprint-str detailed-data)
                     (str "\n```"))]
    (if (> (count content) discord-content-size-limit)
      (str "```edn\n"
           (prn-str detailed-data)
           (str "\n```"))
      content)))

(defn- format-and-report! [loot]
  (let [detailed-content (detailed-loot-content loot)
        {:keys [channel_id id]} (when (<= (count detailed-content) discord-content-size-limit)
                                  (-> (hato/request {:method           :post
                                                     :url              (:detailed-loot-webhook u/config)
                                                     :query-params     {:wait true}
                                                     :throw-exceptions false
                                                     :content-type     :application/json
                                                     :body             (j/write-value-as-string
                                                                         {:content    detailed-content
                                                                          :avatar_url (:discord-avatar u/config)
                                                                          :username   discord-username})})
                                      :body
                                      u/parse-json))]
    (hato/request {:method       :post
                   :url          (:loot-webhook u/config)
                   :content-type :application/json
                   :body         (j/write-value-as-string
                                   {:content    (-> (format-loot loot)
                                                    format-loot-message
                                                    (str "\n\n||" (loot-message-unique-name) "||"))
                                    :embeds     (cond-> []
                                                        id {:title "Full details"
                                                            :type  "link"
                                                            :url   (format "https://discord.com/channels/%s/%s/%s"
                                                                           (:discord-server-id u/config)
                                                                           channel_id
                                                                           id)})
                                    :avatar_url (:discord-avatar u/config)
                                    :username   discord-username})})))

(def ^Executor executor (Executors/newVirtualThreadPerTaskExecutor))

(defn report-loot! [loot]
  (when loot
    (.execute executor ^Runnable #(format-and-report! loot))))

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
  (-> (campaign4.curios/loot-result)
      report-loot!))
