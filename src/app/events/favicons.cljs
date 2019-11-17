(ns app.events.favicons
  (:require [app.macros :refer-macros [defvalidator defevent]]
            [app.api :refer [root]]))

(defvalidator nil)

(defevent :fx :watch-favicons [_ _]
  {:fb-mksub  {:uri "/favicons", :key :favicons-ref, :on-success [:set :favicons]}})

(defevent :fx :change-favicon [_ [_ key value]]
  (if (and (seq key))
    {:fb-save {:uri "/favicons", :id key, :data (if (seq value) value nil)}}))

(defevent :fx :favicons-unsub [{db :db} _]
  {:db       (dissoc db :favicons :favicons-ref)
   :fb-unsub (:favicons-ref db)})
