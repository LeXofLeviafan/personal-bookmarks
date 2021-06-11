(ns app.subs
  (:require [clojure.set :refer [difference]]
            [medley.core :refer [filter-vals map-kv indexed]]
            [re-frame.core :as rf]
            [app.macros :refer-macros [defsub]]
            [app.util :refer [yes favicon-prefix get-path path-tags]]))

(defn- path         [{:keys [node cache]}] (get-path node cache))
(defn- private-tags [db unlocked] (->> db :tags (filter-vals unlocked) keys (map name) set))
(defn- passphrases  [db] (-> db :passphrases set))
(defn- locked-tags  [db] (private-tags db (complement (passphrases db))))
(defn- favicon      [db url] (-> db :favicons (get (favicon-prefix url))))
(defn- favicon-url  [db url]
  (letfn [(join-url [m k] (if-let [v (get m k)] (str k "/" v)))]
    (-> db :favicons (join-url (favicon-prefix url)))))

(defsub :db           [db _] db)
(defsub :user         [db _] (db :user))
(defsub :loading      [db _] (db :loading))
(defsub :auth-error   [db _] (db :auth-error))
(defsub :data         [db _] (db :data))
(defsub :url-form     [db _] (db :url-form))
(defsub :tags-form    [db _] (db :tags-form))
(defsub :login-form   [db _] (db :login-form))
(defsub :confirmation [db _] (db :confirmation))
(defsub :node         [db _] (db :node))
(defsub :cache        [db _] (db :cache))
(defsub :tags         [db _] (db :tags))
(defsub :passphrases  [db _] (db :passphrases))
(defsub :href         [db _] (db :href))
(defsub :scroll       [db _] (db :scroll))
(defsub :to-blank     [db _] (db :to-blank))

(defsub :path          [db _] (path db))
(defsub :path-tags     [db _] (-> db path path-tags))
(defsub :private-tags  [db _] (private-tags db yes))
(defsub :unlocked-tags [db _] (private-tags db (passphrases db)))
(defsub :locked-tags   [db _] (locked-tags db))

(defsub :favicon       [db [_ url]] (favicon db url))
(defsub :favicon-url   [db [_ url]] (favicon-url db url))

(defsub :urls          [db _]
  (let [[data locked?] (map #(% db) [:data locked-tags])]
    (filter-vals #(not (some locked? (:tags %))) data)))

(defsub :path-locked?  [db _]
  (let [locked? (locked-tags db)]
    (some locked? (-> db path path-tags))))

(defsub :first-locked  [db _]
  (let [locked? (locked-tags db)]
    (or (some (fn [[i x]] (and (some locked? (:tags x)) i))
              (-> db path indexed))
        ##Inf)))
