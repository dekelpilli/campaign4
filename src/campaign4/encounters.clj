(ns campaign4.encounters
  (:require
    [campaign4.analytics :as analytics]
    [campaign4.util :as u]
    [clojure.string :as str]
    [randy.core :as r]
    [randy.rng :as rng]))

;TODO redo weathers, random encounters
(def ^:private races ["Aarakocra" "Aasimar" "Bugbear" "Centaur" "Changeling" "Dragonborn" "Dwarf" "Elf" "Firbolg"
                      "Genasi" "Gith" "Gnome" "Goblin" "Goliath" "Half-Elf" "Half-Orc" "Halfling" "Harengon" "Hobgoblin"
                      "Human" "Kalashtar" "Kenku" "Kobold" "Leonin" "Lizardfolk" "Loxodon" "Minotaur" "Orc" "Owlin"
                      "Satyr" "Shifter" "Tabaxi" "Tiefling" "Tortle" "Triton" "Vedalken" "Yuan-Ti Pureblood" "Zeme"])
(def ^:private sexes ["female" "male"])
(def ^:private weather-fns
  (-> {::rain         {::rain         12
                       ::frigid       10
                       ::clear        10
                       ::snow         2
                       ::hail         2
                       ::fog          4
                       ::overcast     10
                       ::thunderstorm 6}
       ::clear        {::rain         8
                       ::clear        18
                       ::frigid       4
                       ::sweltering   16
                       ::fog          1
                       ::overcast     10
                       ::sandstorm    1
                       ::thunderstorm 1}
       ::frigid       {::rain         6
                       ::clear        4
                       ::frigid       8
                       ::snow         5
                       ::hail         6
                       ::fog          3
                       ::overcast     3
                       ::thunderstorm 2}
       ::sweltering   {::rain         2
                       ::clear        12
                       ::sweltering   15
                       ::overcast     4
                       ::thunderstorm 1
                       ::sandstorm    2}
       ::snow         {::rain         3
                       ::clear        4
                       ::frigid       6
                       ::snow         6
                       ::hail         5
                       ::fog          1
                       ::overcast     2
                       ::thunderstorm 1}
       ::hail         {::rain         6
                       ::clear        2
                       ::frigid       10
                       ::snow         3
                       ::hail         8
                       ::fog          3
                       ::overcast     6
                       ::thunderstorm 6}
       ::fog          {::rain         8
                       ::clear        2
                       ::frigid       8
                       ::sweltering   8
                       ::snow         1
                       ::hail         2
                       ::fog          8
                       ::overcast     10
                       ::thunderstorm 3}
       ::overcast     {::rain         10
                       ::clear        9
                       ::sweltering   6
                       ::frigid       8
                       ::hail         1
                       ::fog          7
                       ::overcast     15
                       ::thunderstorm 2}
       ::sandstorm    {::rain       1
                       ::clear      6
                       ::sweltering 12
                       ::fog        1
                       ::sandstorm  6
                       ::acid-rain  1}
       ::acid-rain    {::rain       1
                       ::sweltering 6
                       ::frigid     1
                       ::clear      2
                       ::overcast   4
                       ::sandstorm  2
                       ::acid-rain  3}
       ::thunderstorm {::rain         10
                       ::clear        4
                       ::sweltering   1
                       ::frigid       6
                       ::snow         2
                       ::hail         4
                       ::fog          2
                       ::overcast     8
                       ::thunderstorm 10}}
      (update-vals r/alias-method-sampler)))

(def ^:private generate-random-encounters
  (r/alias-method-sampler
    {[]                90
     [:random]         2
     [:random :random] 8}))

(def ^:private positive-encounters (u/load-data :positive-encounters))

(defn- add-encounter! [type]
  (analytics/record! (str "encounter" type) 1)
  type)

(defn -weather-freqs [n last-weather]
  (when-let [initial-weather (get weather-fns last-weather)]
    (loop [freqs {}
           weather-fn initial-weather
           n n]
      (if (pos? n)
        (let [weather (weather-fn)]
          (recur (update freqs weather (fnil inc 0))
                 (get weather-fns weather)
                 (dec n)))
        freqs))))

(defn pass-time [days initial-weather]
  (analytics/record! "days:other" days)
  (reduce (fn [weather _]
            ((get weather-fns weather)))
          initial-weather
          (range days)))

(defn travel [days last-weather]
  (when-let [initial-weather-fn (get weather-fns last-weather)]
    (loop [acc (sorted-map)
           previous-weather-fn initial-weather-fn
           [day & days] (range 1 (inc days))]
      (let [encounters (cond-> (generate-random-encounters)
                               (u/occurred? 0.1) (conj :positive))
            weather (previous-weather-fn)
            acc (assoc acc day {:weather weather
                                :order   (->> (into encounters (map name) u/characters)
                                              r/shuffle)})]
        (run! add-encounter! encounters)
        (if (seq days)
          (recur acc (get weather-fns weather) days)
          acc)))))

(defn gem-procs []
  (reduce-kv
    (fn [acc char {threshold :gem-threshold}]
      (assoc acc
        (keyword (name char))
        {:default-threshold threshold
         :proc?             (as-> (rng/next-int @r/default-rng 1 101) $
                                  (str (< $ threshold) " (" $ ")"))
         :adv/disadv        (as-> (rng/next-int @r/default-rng 1 101) $
                                  (str (< $ threshold) " (" $ ")"))}))
    {}
    u/character-stats))

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
