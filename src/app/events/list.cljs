(ns app.events.list
  (:require [app.macros :refer-macros [defvalidator defevent]]
            [app.api :refer [root]]))

(defvalidator nil)

(defevent :db :show-loading [db _]
  (merge db {:loading true, :data-ref nil, :data nil}))

(defevent :fx :show-list [{db :db} _]
  {:db        (assoc db :loading false)
   :set-route (:node db)
   :fb-mksub  {:uri        "/urls"
               :query      {:parent (:node db)}
               :key        :data-ref
               :on-success [:set-data]
               :on-failure [:set-data nil]}})

(defevent :fx :set-data [{db :db} [_ value]]
  {:db    (merge db {:data value, :cache (merge (:cache db) value)})
   :debug (clj->js value)
   :emit  [[:watch-node]]})

(defevent :fx :list-unsub [{db :db} _]
  {:db       (dissoc db :data-ref)
   :fb-unsub (:data-ref db)})

(defevent :fx :change-list [{db :db} [_ id]]
  {:db     (merge db {:node id, :data nil})
   :scroll :top
   :emit   [[:list-unsub] [:node-unsub] [:show-list]]})
