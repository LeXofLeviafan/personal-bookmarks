(ns app.macros
  #?(:clj (:import java.util.Base64))
  (:require [re-frame.core :as rf]))

;; build-time

(def ^:private *encode
  #?(:cljs js/btoa
     :clj  #(.encodeToString (Base64/getEncoder) (.getBytes %))))

(def ^:private *decode
  #?(:cljs js/atob
     :clj  #(-> (Base64/getDecoder) (.decode %) str)))

(defmacro require-json [path]
  (let [data (-> path slurp *encode)]
    `(-> ~data *decode js/JSON.parse)))


;; syntactic sugar

(defmacro defsub [name args & body]
  `(rf/reg-sub ~name (fn ~args ~@body)))

(defmacro deffx [name args & body]
  `(rf/reg-fx ~name (fn ~args ~@body)))

(defmacro defevent [type name args & body]  ; defvalidator required
  `(~(case type
      :db `rf/reg-event-db
      :fx `rf/reg-event-fx)
    ~name ~'*spec-validator (fn ~args ~@body)))

(defmacro defvalidator [value]              ; required before defevent (value can be nil)
  `(def ~'*spec-validator ~value))

(defmacro defwrapper [name component & body]
  `(defwrapper* ~name ~'[attrs & content]
     (into [~@(if (keyword? component) [component] [:> component])
            ~@body]
           ~'content)))

(defmacro defwrapper* [name args & body]
  `(defn ~name ~args         ; [attrs & content] presumed
     (let ~'[[attrs content] (if (map? attrs) [attrs content] [{} (cons attrs content)])]
       ~@body)))

(defmacro succeed! [& args]  ; on-success required
  `(and ~'on-success (rf/dispatch (conj ~'on-success ~@args))))

(defmacro fail! [& args]     ; on-failure required
  `(and ~'on-failure (rf/dispatch (conj ~'on-failure ~@args))))

(defmacro try-methods [o & keys]
  `(cond ~@(mapcat (fn [k] `[(aget ~o ~(name k)) (. ~o ~k)]) keys)))
