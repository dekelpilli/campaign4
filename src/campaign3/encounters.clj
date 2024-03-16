(ns campaign3.encounters
  (:require (campaign3
              [db :as db]
              [helmets :as helmets]
              [prompting :as p]
              [util :as u])
            [clojure.string :as str]
            [randy.core :as r]
            [randy.rng :as rng]))

(def ^:private extra-loot-threshold 13)
(def ^:private extra-loot-step 2)
(def ^:private races ["Aarakocra" "Aasimar" "Bugbear" "Centaur" "Changeling" "Dragonborn" "Dwarf" "Elf" "Firbolg"
                      "Genasi" "Gith" "Gnome" "Goblin" "Goliath" "Half-Elf" "Half-Orc" "Halfling" "Harengon" "Hobgoblin"
                      "Human" "Kalashtar" "Kenku" "Kobold" "Leonin" "Lizardfolk" "Loxodon" "Minotaur" "Orc" "Owlin"
                      "Satyr" "Shifter" "Tabaxi" "Tiefling" "Tortle" "Triton" "Vedalken" "Yuan-Ti Pureblood"])
(def ^:private sexes ["female" "male"])

(def ^:private activities [:befriend-creature :busk :chronicle :entertain :force-march :gather-components
                           :gossip :harvest :pray :rob :scout :seek-shelter])
(def ^:private weather-fns
  (-> {:rain         {:rain         12
                      :frigid       10
                      :clear        10
                      :snow         2
                      :hail         2
                      :fog          4
                      :overcast     10
                      :thunderstorm 6}
       :clear        {:rain         8
                      :clear        18
                      :frigid       4
                      :sweltering   16
                      :fog          1
                      :overcast     10
                      :sandstorm    1
                      :thunderstorm 1}
       :frigid       {:rain         6
                      :clear        4
                      :frigid       8
                      :snow         5
                      :hail         6
                      :fog          3
                      :overcast     3
                      :thunderstorm 2}
       :sweltering   {:rain         2
                      :clear        12
                      :sweltering   15
                      :overcast     4
                      :thunderstorm 1
                      :sandstorm    2}
       :snow         {:rain         3
                      :clear        4
                      :frigid       6
                      :snow         6
                      :hail         5
                      :fog          1
                      :overcast     2
                      :thunderstorm 1}
       :hail         {:rain         6
                      :clear        2
                      :frigid       10
                      :snow         3
                      :hail         8
                      :fog          3
                      :overcast     6
                      :thunderstorm 6}
       :fog          {:rain         8
                      :clear        2
                      :frigid       8
                      :sweltering   8
                      :snow         1
                      :hail         2
                      :fog          8
                      :overcast     10
                      :thunderstorm 3}
       :overcast     {:rain         10
                      :clear        9
                      :sweltering   6
                      :frigid       8
                      :hail         1
                      :fog          7
                      :overcast     15
                      :thunderstorm 2}
       :sandstorm    {:rain       1
                      :clear      6
                      :sweltering 12
                      :fog        1
                      :sandstorm  6
                      :acid-rain  1}
       :acid-rain    {:rain       1
                      :sweltering 6
                      :frigid     1
                      :clear      2
                      :overcast   4
                      :sandstorm  2
                      :acid-rain  3}
       :thunderstorm {:rain         10
                      :clear        4
                      :sweltering   1
                      :frigid       6
                      :snow         2
                      :hail         4
                      :fog          2
                      :overcast     8
                      :thunderstorm 10}}
      (update-vals r/alias-method-sampler)))

(def ^:private generate-random-encounters
  (r/alias-method-sampler
    {[]                90
     [:random]         2
     [:random :random] 8}))

(def ^:private positive-encounters (db/load-all :positive-encounters))

(defn- add-encounter! [type]
  (u/record! (str "encounter" type) 1)
  type)

(defn -weather-freqs [n]
  (when-let [initial-weather (p/>>item "What was the weather yesterday?" weather-fns)]
    (loop [freqs {}
           weather-fn initial-weather
           n n]
      (if (pos? n)
        (let [weather (weather-fn)]
          (recur (update freqs weather (fnil inc 0))
                 (get weather-fns weather)
                 (dec n)))
        freqs))))

(defn pass-time [days]
  (when-let [initial-weather (p/>>item "What was the weather yesterday?" (keys weather-fns))]
    (u/record! "days:other" days)
    (reduce (fn [weather _]
              ((get weather-fns weather)))
            initial-weather
            (range days))))

(defn travel [days]
  (when-let [initial-weather-fn (p/>>item "What was the weather yesterday?" weather-fns)]
    (u/record! "days:travel" days)
    (loop [acc (sorted-map)
           previous-weather-fn initial-weather-fn
           [day & days] (range 1 (inc days))]
      (let [encounters (cond-> (generate-random-encounters)
                               (u/occurred? 0.1) (conj :positive))
            weather (previous-weather-fn)
            acc (assoc acc day {:weather weather
                                :order   (->> (keys helmets/character-enchants)
                                              (into encounters)
                                              r/shuffle)})]
        (run! add-encounter! encounters)
        (if (seq days)
          (recur acc (get weather-fns weather) days)
          acc)))))

(defn- calculate-loot [difficulty investigations]
  (let [extra-loot-sum (transduce (map (fn [s] (- (parse-long s) extra-loot-threshold))) + 0 investigations)
        dungeon? (when-not (#{:mild :bruising} difficulty)
                   (p/>>item "In a dungeon?" [true false] :none-opt? false))
        base-loot (case difficulty
                    (:mild :bruising) 0
                    :bloody (if dungeon? 0 1)
                    :brutal (if dungeon? 1 2)
                    (:boss :oppressive) (if dungeon? 2 4)
                    :overwhelming (if dungeon? 3 5)
                    :crushing (if dungeon? 4 6)
                    :devastating (if dungeon? 5 8))]
    (->> (count investigations)
         (* extra-loot-step)
         (/ extra-loot-sum)
         double
         Math/round
         (max 0)
         (+ base-loot))))

(defn encounter-rewards []
  (u/when-let* [difficulty (p/>>item "Difficulty:" [:mild :bruising :bloody :brutal :oppressive :overwhelming :crushing :devastating :boss] :sorted? false)
                investigations (some-> (p/>>input "List investigations:")
                                       (str/split #","))]
    {:xp   (case difficulty
             :mild (+ 6 (rng/next-int @r/default-rng 2))
             :bruising (+ 7 (rng/next-int @r/default-rng 2))
             :bloody (+ 8 (rng/next-int @r/default-rng 2))
             :brutal (+ 10 (rng/next-int @r/default-rng 3))
             :oppressive (+ 13 (rng/next-int @r/default-rng 3))
             (:boss :overwhelming) (+ 14 (rng/next-int @r/default-rng 3))
             :crushing (+ 16 (rng/next-int @r/default-rng 3))
             :devastating (+ 18 (rng/next-int @r/default-rng 3)))
     :loot (calculate-loot difficulty investigations)}))

(defn positive-encounter []
  {:race      (r/sample races)
   :sex       (r/sample sexes)
   :encounter (r/sample positive-encounters)})

(defn- num-char->num [c]
  (- (int c) 48))

(defn cheiro-sum [word maximum]
  (let [char-values {\a 1 \b 2 \c 3 \d 4 \e 5
                     \f 8 \g 3 \h 5 \i 1 \j 1
                     \k 2 \l 3 \m 4 \n 5 \o 7
                     \p 8 \q 1 \r 2 \s 3 \t 4
                     \u 6 \v 6 \w 6 \x 5 \y 1 \z 7}
        sum (transduce (map char-values) + 0 (str/lower-case word))]
    (loop [sum sum]
      (if (or (<= sum maximum) (< sum 10))
        sum
        (recur (transduce (map num-char->num) + 0 (str sum)))))))
