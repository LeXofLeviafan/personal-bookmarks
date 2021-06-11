(ns app.db
  (:require [clojure.string :as s]
            [app.api :refer [root]]
            [app.util :refer [hash-route]]))

;; initial state of app-db
(def app-db {:loading     (boolean (js/localStorage.getItem :logon))
             :node        (hash-route)
             :scroll      js/document.body.scrollTop
             :to-blank    true
             :passphrases #{}})
