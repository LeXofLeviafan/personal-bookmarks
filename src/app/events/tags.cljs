(ns app.events.tags
  (:require [medley.core :refer [filter-vals]]
            [re-frame.core :as rf]
            [app.macros :refer-macros [defvalidator defevent]]
            [app.util :refer [encode]]
            [app.fx :refer [storage-get]]))

(defvalidator nil)


(defevent :fx :watch-tags [_ _]
  {:fb-mksub  {:uri "/tags", :key :tags-ref, :on-success [:set-tags]}})

(defevent :fx :tags-unsub [{db :db} _]
  {:db       (dissoc db :passwords :tags :tags-ref)
   :fb-unsub (:tags-ref db)})

(defevent :db :tags-dialog [db _]
  (assoc db :tags-form {}))

(defevent :fx :close-tags-dialog [{db :db} _]
  (let [tags-form (:tags-form db), tag (:tag tags-form), passphrase (:passphrase tags-form)]
    (merge {:db (dissoc db :tags-form)}
      (cond (and (seq tag) (seq passphrase)) {:emit [[:create-tag tag passphrase]]}
            (seq passphrase)                 {:emit [[:add-passphrase passphrase]]}))))

(defevent :fx :set-tags [{db :db} [_ value]]
  {:db  (update (assoc db :tags value) :tags-form dissoc :error)})


(defevent :fx :save-passphrases [{db :db} _]
  {:encrypt-data {:data       (assoc (storage-get :logon) :data (get db :passphrases #{}))
                  :on-success [:storage-set :tags]}})

(defn- set-passphrases [db pwds tags-form]
  {:db   (merge db {:tags-form tags-form, :passphrases (if (seqable? pwds) pwds #{})})
   :emit [[:save-passphrases]]})

(defevent :fx :set-passphrases [{db :db} [_ pwds]] (set-passphrases db pwds nil))

(defevent :fx :add-passphrase [{db :db} [_ value tags-form]]
  (let [pwds (conj (-> db :passphrases set) (encode value))]
    (if (seq value)
      (set-passphrases db pwds tags-form)
      {:db (assoc-in db [:tags-form :error] "Empty passphrase")})))

(defevent :fx :del-passphrase [{db :db} [_ passphrase]]
  (set-passphrases db (disj (:passphrases db) passphrase) {}))


(defevent :fx :lock-tag [{db :db} [_ tag]]
  (let [passphrase (-> db :tags (get tag))]
    (set-passphrases db (disj (:passphrases db) passphrase) {})))

(defn- edit-tag [{db :db} [evt tag passphrase tag-form]]
  (let [pwd         (and (seq passphrase) (encode passphrase))
        tag-pwd     (get-in db [:tags tag])
        passphrases (-> db :passphrases set)
        unlocked    (or (nil? tag-pwd) (= pwd tag-pwd) (passphrases tag-pwd))
        replaced    (-> passphrases (disj tag-pwd) ((if (= evt :create-tag) conj disj) pwd))]
    (if-not (and (seq tag) (seq pwd) unlocked)
      {:db (assoc-in db [:tags-form :error] "Tag action failed")}
      (merge (set-passphrases db replaced tag-form)
             {:fb-save {:uri "/tags", :id tag, :data (if (= evt :create-tag) pwd)}}))))

(rf/reg-event-fx :remove-tag edit-tag)
(rf/reg-event-fx :create-tag edit-tag)
