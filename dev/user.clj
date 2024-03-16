(ns user
  (:require
    [clojure.string :as str]
    [clojure.tools.namespace.repl :refer [refresh]]
    [org.fversnel.dnddice.core :as d]
    [honey.sql :as hsql]
    [randy.core :as r]
    [campaign4.amulets :as amulets]
    [campaign4.core :as c]
    [campaign4.crafting :as crafting]
    [campaign4.curios :as curios]
    [campaign4.db :as db]
    [campaign4.db-data :as dbd]
    [campaign4.enchants :as e]
    [campaign4.encounters :as encounters]
    [campaign4.helmets :as helmets]
    [campaign4.paths :as paths]
    [campaign4.prep :as prep]
    [campaign4.prompting :as p]
    [campaign4.randoms :as randoms]
    [campaign4.relics :as relics]
    [campaign4.rings :as rings]
    [campaign4.tarot :as tarot]
    [campaign4.uniques :as uniques]
    [campaign4.util :as u])
  (:use
    [campaign4.amulets]
    [campaign4.core]
    [campaign4.crafting]
    [campaign4.curios]
    [campaign4.db]
    [campaign4.db-data]
    [campaign4.enchants]
    [campaign4.encounters]
    [campaign4.helmets]
    [campaign4.paths]
    [campaign4.prep]
    [campaign4.prompting]
    [campaign4.randoms]
    [campaign4.relics]
    [campaign4.rings]
    [campaign4.tarot]
    [campaign4.uniques]
    [campaign4.util]))

;https://asciinema.org/a/296507
(def safe-requires
  "List of namespaces to require and refer when inside user ns at load time.
   Can be given an initialization body to execute after having been required.
   To do so, wrap the lib spec in a vector, and all elements after the lib
   spec vector will be evaled after the lib spec has been required."
  '[[clojure.repl :as repl :refer (source apropos dir pst doc find-doc)]
    [clojure.java.javadoc :as javadoc :refer (javadoc)]
    [clojure.pprint :as pprint :refer (pp pprint)]
    [clojure.stacktrace :as stacktrace :refer (e)]
    [clojure.set :as set]
    [[io.aviso.repl :as aviso]
     ((resolve 'aviso/install-pretty-exceptions))]
    [[puget.printer :as puget :refer (cprint)]
     (add-tap (bound-fn* (resolve 'puget/cprint)))]])

(defn safe-require-init [req]
  (let [init? (-> req first vector?)
        lib (if init? (first req) req)
        init (when init? (rest req))]
    `(try
       (require '~lib)
       ~@init
       (catch Throwable t#
         (println ~(str "Error loading " lib ":")
                  (or (.getMessage t#)
                      (type t#)))))))
(defmacro safe-require-inits []
  `(do
     ~@(for [req safe-requires]
         (safe-require-init req))))
(safe-require-inits)

(defmacro cp [] `(cprint *1))

(def default-aliases (filter (comp #(str/starts-with? % "campaign3") str val)
                             (ns-aliases *ns*)))

(defmacro reload []
  (cons 'do
        (reduce (fn [l [alias-sym ns-sym]]
                  (->> l
                       (cons (list `require [`'~ns-sym :as `'~alias-sym]))
                       (cons (list `use [`'~ns-sym :as `'~alias-sym]))
                       (cons (list `ns-unalias `*ns* `'~alias-sym))))
                '()
                (update-vals default-aliases (comp symbol str)))))

(defn rr []
  (let [refreshed (refresh)]
    (if (= :ok refreshed)
      (reload)
      refreshed)))

(defn roll [n x]
  (-> (str n \d x)
      d/roll))
