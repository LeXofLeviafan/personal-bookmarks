(ns app.fx.firebase
  (:require ["firebase/app" :as firebase]
            ["firebase/auth"]
            ["firebase/database"]
            ["firebase/storage"]
            [clojure.string :as s]
            [clojure.walk :as w]
            [clojure.core.async :refer-macros [go]]
            [cljs-promises.async :refer-macros [<?]]
            [oops.core :refer [ocall]]
            [medley.core :refer [map-keys map-vals]]
            [re-frame.core :as rf]
            [app.macros :refer-macros [require-json deffx succeed! fail!]]
            [app.util :refer [->key key->]]
            [app.fx.token :refer [encrypt-token]]))

(defonce setup (firebase/initializeApp (require-json "firebase.json")))

(defonce db (firebase/database))
(defonce storage (firebase/storage))

(defn auth [{:keys [user passwd]}]
  (-> (firebase/auth)
      (ocall :signInWithEmailAndPassword user passwd)
      js/Promise.resolve))

(defn logout [] (js/Promise.resolve (ocall (firebase/auth) :signOut)))

(defn convert [snapshot]
  (->> snapshot .val js->clj (map-keys key->) (map-vals w/keywordize-keys)))

(defn mkref [& uri]
  (ocall db :ref (s/join "/" uri)))

(defn filtered [ref query]
  (if-let [key (-> query keys first)]
    (.. ref (orderByChild (name key)) (equalTo (get query key)))
    ref))

(defn sub [{:keys [ref query on-success on-failure]}]
  (let [ref* (filtered ref query)]
    (. ref* on "value" #(if-let [x (convert %)] (succeed! x) (fail! "Data not found")))))

(defn storage-ref [path] (.. storage ref (child path)))


(deffx :fb-auth [{:keys [cred on-success on-failure]}]
  (go (try
        (-> cred auth <?)
        (-> cred encrypt-token <? succeed!)
        (catch js/Error e
           (js/console.error (ex-message e))
           (fail! (ex-message e))))))

(deffx :fb-logout [on-success]
  (go (<? (logout)) (succeed!)))


(deffx :fb-mkref [{:keys [uri on-success]}]
  (succeed! (mkref uri)))

(rf/reg-fx :fb-sub sub)

(deffx :fb-mksub [{:keys [uri key] :as options}]
  (let [ref (mkref uri)]
    (and key (rf/dispatch [:set key ref]))
    (sub (merge options {:ref ref}))))

(deffx :fb-unsub [ref]
  (and ref (. ref off)))

(deffx :fb-fetch [{:keys [uri on-success]}]
  (go (let [ref (mkref uri)]
        (-> (. ref once "value") <? convert succeed!)
        (. ref off))))

(deffx :fb-save [{:keys [uri id data]}]
  (cond (seq id)  (ocall (mkref uri (->key id)) :set data)
        (nil? id) (ocall (mkref uri) :push data)))

(deffx :fb-del [path]
  (ocall (mkref path) :remove))


(deffx :upload-file [{:keys [path file on-success on-failure]}]
  (go (try
        (let [ref (storage-ref path)]
          (-> ref (.put file) js/Promise.resolve <?)
          (-> ref .getDownloadURL js/Promise.resolve <? succeed!))
        (catch js/Error e
          (js/console.error (ex-message e))
          (fail! (ex-message e))))))

(deffx :remove-files [{:keys [paths on-success on-failure]}]
  (go (try
        (doseq [s paths] (-> s storage-ref .delete js/Promise.resolve <?))
        (succeed!)
        (catch js/Error e
          (js/console.error (ex-message e))
          (fail! (ex-message e))))))
