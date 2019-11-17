(ns app.fx
  (:require [clojure.core.async :refer-macros [go]]
            [cljs.reader :as edn]
            [cljs-promises.async :refer-macros [<?]]
            [oops.core :refer [oget ocall oset!]]
            [re-frame.core :as rf]
            [app.macros :refer-macros [deffx succeed! fail! try-methods]]
            [app.db :refer [app-db]]
            [app.fx.firebase]
            [app.fx.token]))

(deffx :emit [events]
  (dorun (map rf/dispatch events)))

(deffx :print [message]
  (print message))

(deffx :debug [message]
  (js/console.debug message))

(deffx :log [message]
  (js/console.log message))

(deffx :warn [message]
  (js/console.warn message))

(deffx :error [message]
  (js/console.error message))

(deffx :storage-set [m]
  (doseq [[k v] m]
    (js/localStorage.setItem k v)))

(deffx :storage-clear []
  (js/localStorage.clear))

(deffx :set-route [s]
  (oset! js/location :hash s))

(def storage-get #(-> % js/localStorage.getItem edn/read-string))

(deffx :scroll [pos]
  (oset! js/document "body.scrollTop" (cond (number? pos)   pos
                                            (= pos :bottom) js/document.body.scrollHeight
                                            :else           0)))


;; these actions only work inside onclick event processors

(defn open-file [mime on-success]
  (let [input (js/document.createElement "input")]
    (js/Object.assign input #js{:type "file", :accept mime})
    (oset! input "onchange" #(when-let [file (oget input "files.0")]
                              (succeed! file)))
    (. input click)))

(defn toggle-fullscreen []
  (if js/document.fullscreen
    (try-methods js/document exitFullscreen mozCancelFullScreen webkitExitFullscreen)
    (try-methods js/document.body requestFullscreen mozRequestFullScreen webkitRequestFullscreen)))
