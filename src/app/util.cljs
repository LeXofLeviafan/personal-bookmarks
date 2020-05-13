(ns app.util
  (:require ["jssha" :as js-sha]
            ["qrcode-generator" :as qr]
            [clojure.string :as s]
            [oops.core :refer [oget ocall]]
            [medley.core :refer [map-keys map-vals find-first]]
            [re-frame.core :as rf]
            [app.api :refer [root]]))


(def non-key ".#$[]/%")		; Chars not allowed in Firebase key (+\% as service char)
(def non-key* #"[.#$\[\]/%]")

(defn pad0 [s n] (.padStart (str s) n \0))
(defn hex [c] (-> c (.codePointAt 0) (.toString 16) (pad0 2)))
(defn chr [h] (-> h (.slice 1) (js/parseInt 16) String/fromCharCode))

(defn ->key [s] (s/replace s non-key* #(str \% (hex %))))
(defn key-> [s] (s/replace s #"%[0-9a-f]{2}" chr))

(defn upper? [s]
  (= s (s/upper-case s)))

(defn kw->caps [kw]
  (-> kw name s/upper-case (s/replace #"-" "_")))

(defn caps->kw [s]
  (-> s s/lower-case (s/replace #"_" "-") keyword))

(def yes (constantly true))

(defn obj->map [o & {filter-by :filter :or {filter-by yes}}]
  (->> (js/Object.keys o)
       (filter filter-by)
       (map (fn [k] [k (aget o k)]))
       (into {})))

(defn enum->map [enum]
  (map-keys caps->kw (obj->map enum :filter upper?)))


(let [month ["January" "February" "March" "April" "May" "June" "July" "August" "Septemper" "October" "November" "December"]
      mon   (mapv #(.slice % 0 3) month)
      fmts  {:YYYY #(-> % .getFullYear  (pad0 4))
             :MM   #(-> % .getMonth inc (pad0 2))
             :MMM  #(-> % .getMonth mon)
             :MMMM #(-> % .getMonth month)
             :DD   #(-> % .getDate      (pad0 2))
             :HH   #(-> % .getHours     (pad0 2))
             :mm   #(-> % .getMinutes   (pad0 2))}]
  (defn date-format [format]
    (fn [date]
      (if-let [date* (and date (js/Date. date))]
        (s/join (map #(if-not (keyword? %) % ((fmts %) date*)) format))))))


(defn force-evt [& args]
  (fn [evt]
    (apply rf/dispatch args)
    (ocall evt :preventDefault)))

(defn *value [event] #(let [x (if (array? %) % (oget % "target.value"))]
                        (rf/dispatch-sync (conj event x))))
(defn *html [& text] {:dangerously-set-inner-HTML {:__html (apply str text)}})

(defn merge-class [& css-classes]
  (if-let [res (->> css-classes
                    (mapcat #(cond (keyword? %) [%]
                                   (vector? %)  %
                                   (string? %)  (s/split #"\s*" %)))
                    (filter #(or (keyword? %) (seq %)))
                    seq)]
    (vec res)))
(defn merge* [& attrs]
  (merge (apply merge attrs) {:class (apply merge-class (map :class attrs))}))


(defn title [{:keys [name url]}] (or (find-first not-empty [name url]) ""))
(def title* (comp s/lower-case title second))

(defn favicon-prefix [url]
  (and url (re-find #"https?://[^/]+" url)))

(def show-date (date-format [:YYYY \- :MMM \- :DD " " :HH ":" :mm]))

(defn updated [{created :created, updated* :updated}]
  (and (not= created updated*) updated*))

(defn details [{:keys [description links created] :as data}]
  (letfn [(date [prefix x] (if-not x "" (str "[" prefix " " (show-date x) "]")))]
    (->> [description
          (and links (s/join "\n * " (concat ["Links:"] links)))
          (str (date "added" created) " " (date "updated" (updated data)))]
         (filter seq)
         (s/join "\n\n"))))


(defn ->vector [v]
  (if-not (map? v) (vec v) (mapv second (sort v))))

(defn splice [v i]
  (vec (concat (subvec v 0 i) (subvec v (inc i)))))


(defn fix-url [s]
  (cond (empty? s)  nil
        (string? s) (if (re-matches #"[a-zA-Z]+://.*" s) s (str "http://" s))
        (map? s)    (map-vals fix-url s)
        :else       (filterv #(-> % empty? not) (map fix-url s))))

(defn fix-urls [ks m]
  (reduce #(update %1 %2 fix-url) m ks))

(defn fix-sets [ks m]
  (reduce #(update %1 %2 (comp vec distinct)) m ks))

(defn add-dates [m]
  (let [now (.toJSON (js/Date.))]
    (merge {:created now} m {:updated now})))


(defn get-path [id cache]
  (letfn [(get-path* [id path]
            (if (= id root)
              (reverse (conj path {:id root}))
              (if-let [x (get cache id)]
                (recur (:parent x) (conj path (assoc x :id id))))))]
    (get-path* id [])))

(defn path-tags [path]
  (distinct (mapcat :tags path)))


(defn hash-route []
  (let [s js/location.hash]
    (if (empty? s) root (s/replace s #"^#" ""))))


(defn qrcode [s]
  (-> (doto (qr. 0 \H)
        (.addData s)
        .make)
      .createDataURL))


(defn encode [s]
  (-> (doto (js-sha. "SHA-1" "TEXT")
        (.update :bookmarks)
        (.update s))
      (.getHash "HEX")))
(aset js/window "sha" encode)
