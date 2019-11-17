(ns script.backup
  (:require fs [firebase-admin :as firebase]))

(def config (require "./firebase.json"))
(def token  (require "./firebase-token.json"))

(def now  (.to-JSON (Date.)))
(def file (str "backups/" now ".json"))

(firebase.initialize-app {:credential  (firebase.credential.cert token)
                          :databaseURL (:databaseURL config)})

(-> (firebase.database)
    (.ref "/")
    (.once :value
      (fn [snapshot]
        (fs.mkdir-sync "backups" {:recursive true})
        (fs.write-file-sync file (-> snapshot .val (JSON.stringify nil 2) (str "\n")))
        (process.exit))))
