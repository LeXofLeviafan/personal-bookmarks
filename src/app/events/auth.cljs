(ns app.events.auth
  (:require [app.macros :refer-macros [defvalidator defevent]]
            [app.fx :refer [storage-get]]))

(defvalidator nil)


(defevent :fx :fb-init [_ [_ logon]]
  {:emit          [[:show-loading]]
   :decrypt-data  {:data (assoc logon :data (js/localStorage.getItem :tags))
                   :on-success [:set-passphrases]
                   :on-failure [:error :oops]}
   :decrypt-token {:data logon
                   :on-failure [:fb-init-fail]
                   :on-success [:fb-auth]}})

(defevent :fx :fb-init-fail [{db :db} [_ reason]]
  {:emit [[:clear-data]
          [:error :auth-error reason]]
   :db   (merge db {:loading false})})

(defevent :fx :fb-auth [_ [_ cred]]
  {:fb-auth {:cred       cred
             :on-failure [:error :auth-error]
             :on-success [:save-token]}})

(defevent :fx :save-token [{:keys [db]} [_ token]]
  {:storage-set {:logon token}
   :db          (merge db {:user (:user token)})
   :emit        [[:save-passphrases] [:show-list] [:watch-tags] [:watch-favicons]]})

(defevent :fx :logon [{:keys [db]} _]
  {:emit [[:fb-auth (:login-form db)]]})


(defevent :fx :logout [{db :db} _]
  {:db        (dissoc db :data)
   :fb-logout [:clear-data]
   :emit      [[:list-unsub] [:node-unsub] [:tags-unsub] [:favicons-unsub]]})

(defevent :fx :clear-data [{db :db} _]
  {:storage-clear nil
   :db            (assoc db :user nil)})
