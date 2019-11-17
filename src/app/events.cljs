(ns app.events
  (:require [app.macros :refer-macros [defvalidator defevent]]
            [app.api :refer [root]]
            [app.db :refer [app-db]]
            [app.fx]
            [app.events.auth]
            [app.events.list]
            [app.events.url]
            [app.events.tags]
            [app.events.favicons]
            [app.util :refer [qrcode]]))

(defvalidator nil)

(defevent :db :initialize-db [_ _] app-db)

(defevent :db :set [db [_ key value]]
  (assoc db key value))

(defevent :db :set-in [db [_ path value]]
  (assoc-in db path value))

(defevent :db :update [db [_ key func & args]]
  (apply update db func args))

(defevent :fx :error [{db :db} [_ key error]]
  {:error error
   :db    (assoc db key error)})

(defevent :fx :storage-set [_ [_ k v]]
  {:storage-set {k v}})

(defevent :db :set-href [db [_ url]]
  (assoc db :href (and (seq url) (qrcode url))))

(defevent :fx :scroll [_ [_ pos]]
  {:scroll pos})
