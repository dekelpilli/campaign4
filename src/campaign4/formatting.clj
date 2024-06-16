(ns campaign4.formatting
  (:require
    [campaign4.randoms :as randoms]
    [selmer.node :as node]
    [selmer.parser :as selmer]
    [selmer.util :as su])
  (:import
    (java.io StringReader)))

(defn- regurgitate-tag [tag-value]
  (if tag-value
    (str "{{" tag-value "}}")
    ""))

(defn- regurgitating-missing-value-formatter [{:keys [tag-value]} _opts]
  (regurgitate-tag tag-value))

(defn- randoms-filter [_ preset & args]
  ((randoms/randoms->fn (keyword preset) args)))

(selmer/add-filter! :random randoms-filter)

(defn -render-template
  "selmer.parser/render-template, but adds not-empty around render-node to make it work on filters (which return \"\")"
  [template context-map]
  (let [buf (StringBuilder.)]
    (doseq [^node/INode element template]
      (if-let [value (-> (node/render-node element context-map)
                         not-empty)]
        (.append buf value)
        (do (println (:tag (meta element)))
            (.append buf (regurgitate-tag (-> element meta :tag :tag-value))))))
    (.toString buf)))

(defn load-mod [{:keys [effect] :as mod}]
  (try
    (with-open [reader (StringReader. effect)]
      (let [parsed (selmer/parse selmer/parse-input reader)]
        (-> (assoc mod :template parsed)
            (dissoc :effect))))
    (catch Exception e
      (throw (ex-info "Failed to load mod" mod e)))))

(defn display-mod
  ([mod] (display-mod mod {}))
  ([{:keys [template] :as mod} context]
   (binding [su/*escape-variables* false
             su/*missing-value-formatter* regurgitating-missing-value-formatter]
     (let [generated (-render-template template context)]
       (-> (assoc mod :effect generated)
           (dissoc :template))))))

(comment
  (->> (load-mod {:effect "You gain {{x|alwaysnil}} and {{x|random:feats}}"})
       display-mod))
