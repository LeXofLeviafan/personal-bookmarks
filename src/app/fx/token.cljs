(ns app.fx.token
  (:require [clojure.string :as s]
            [clojure.core.async :refer-macros [go]]
            [cljs.reader :as edn]
            [cljs-promises.async :refer-macros [<?]]
            [oops.core :refer [oget+]]
            [re-frame.core :as rf]
            [app.macros :refer-macros [deffx succeed! fail!]]))


(defonce sw-info
  (s/join \space (map #(oget+ js/navigator %)
                      [:appCodeName :appName :appVersion :vendor :product :productSub :platform])))

(defn- hw-info! []
  (let [gl    (.. js/document (createElement "canvas") (getContext "webgl"))
        dbg   (and gl (. gl getExtension "WEBGL_debug_renderer_info"))]
    (and gl (->> ["VENDOR" "RENDERER"]
                 (map (fn [s] [(oget+ gl s) (when dbg (oget+ dbg (str "UNMASKED_" s "_WEBGL")))]))
                 (map (fn [ks] (->> ks (map #(and % (. gl getParameter %))) (s/join " | "))))
                 (s/join \newline)))))

(defonce hw-info (or (hw-info!) (hw-info!) (hw-info!) sw-info))  ; sometimes requesting WebGL fails for no reason
(js/console.debug hw-info)


(def chr js/String.fromCharCode)
(def ord #(. % charCodeAt 0))
(def split #(s/split % ""))

(defn to-base64 [buf] (->> buf js/Uint8Array. js/Array.from (map chr) (s/join "") js/btoa))
(defn from-base64 [s] (->> s js/atob split (map ord) js/Uint8Array.))

(def to-bytes   (let [o (js/TextEncoder.)] (.bind o.encode o)))
(def from-bytes (let [o (js/TextDecoder.)] (.bind o.decode o)))

(defn base-key []
  (js/crypto.subtle.importKey "raw" (to-bytes hw-info) #js{:name "PBKDF2"}
                              false #js["deriveBits" "deriveKey"]))
(defn crypt-key [user]
  (. (base-key) then #(js/crypto.subtle.deriveKey #js{:name "PBKDF2", :salt (to-bytes user)
                                                      :iterations 100000, :hash "SHA-256"}
                                                  % #js{:name "AES-GCM", :length 256}
                                                  true #js["encrypt" "decrypt"])))

(defn algo [time] #js{:name "AES-GCM", :iv (to-bytes time)})


(defn- encrypt [time user text]
  (.then (crypt-key user) #(js/crypto.subtle.encrypt (algo time) % (to-bytes text))))

(defn- decrypt [time user data]
  (.then (crypt-key user) #(js/crypto.subtle.decrypt (algo time) % (from-base64 data))))


(defn encrypt-token [{:keys [user passwd]}]
  (let [time (js/Date)]
    (.then (encrypt time user passwd) (fn [token] {:user user, :time time, :token (to-base64 token)}))))

(defn encrypt-data [{:keys [user time data]}]
  (.then (encrypt time user data) to-base64))

(defn decrypt-token [{:keys [user time token]}]
  (.then (decrypt time user token) (fn [passwd] {:user user, :passwd (from-bytes passwd)})))

(defn decrypt-data [{:keys [user time data]}]
  (.then (decrypt time user data) from-bytes))


(deffx :encrypt-token [{:keys [data on-success]}]
  (-> data encrypt-token <? succeed! go))

(deffx :encrypt-data [{:keys [data on-success]}]
  (-> data encrypt-data <? succeed! go))

(deffx :decrypt-token [{:keys [data on-success on-failure]}]
  (go (try (-> data decrypt-token <? succeed!)
        (catch js/Error e (fail! "Failed to decrypt token")))))

(deffx :decrypt-data [{:keys [data on-success on-failure]}]
  (go (try (and (:data data) (-> data decrypt-data <? edn/read-string succeed!))
        (catch js/Error e (fail! "Failed to decrypt data")))))
