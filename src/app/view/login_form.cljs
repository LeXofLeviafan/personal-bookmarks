(ns app.view.login-form
  (:require ["@blueprintjs/core" :as bp]
            [re-frame.core :as rf]
            [app.util :refer [force-evt]]
            [app.view.components :as comp]))

(defn login-form []
  (let [{:keys [user passwd]} @(rf/subscribe [:login-form])
        error                 @(rf/subscribe [:auth-error])]
    [:form.login
      (and error [:> bp/Callout {:intent :danger} error])
      [comp/input :login-form :user   {:type :email,    :value user,   :required true}]
      [comp/input :login-form :passwd {:type :password, :value passwd, :required true}]
      [:> bp/Button {:intent :primary, :type :submit, :on-click (force-evt [:logon])}
        "Log in"]]))
