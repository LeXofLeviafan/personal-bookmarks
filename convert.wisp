(ns script.convert
  (:require [wisp.compiler :as wisp]
            [wisp.runtime :refer [inc dictionary? merge =]]
            [wisp.sequence :refer [first second last empty? map map-indexed sort assoc into reduce]]
            fs
            [minimist :as parse-args]))

(def script-args (let [i (.index-of process.argv "+")]
                   (if (< i 0) [] (.slice process.argv (inc i)))))

(def args (parse-args script-args {:default {:output "-", :collection "urls"}
                                   :alias   {:o :output, :c :collection, :h :help}
                                   :string  [:output :collection]}))

(when (or args.help (empty? args._))
  (print (str "USAGE: convert.wisp + [-o|--output filename] [-c|--collection key] conversions...\n"
              "  Takes last backup file and applies conversions (wisp function definitions (fn [item key db]))\n"
              "  to every item of a collection (default: 'urls').\n"
              "  Imported namespaces are: wisp.runtime (r/), wisp.sequence (l/), wisp.string (s/)."))
  (process.exit))


(def requires "(ns eval (:require [wisp.runtime :as r :refer [=]]
                                  [wisp.sequence :as l]
                                  [wisp.string :as s]))")

(def last-backup (-> (fs.readdir-sync "backups") sort last))
(def data (-> (str "backups/" last-backup) fs.read-file-sync JSON.parse))

(defn dict [coll]
  (if (dictionary? coll) coll (into {} (map-indexed vector (vec coll)))))

(defn convert [coll fstr]
  (let [f (wisp.evaluate (str requires fstr))]
    (into {} (map (fn [e] [(first e) (f (second e) (first e) data)])
                  (dict coll)))))

(def result (assoc data args.collection
               (reduce convert (get data args.collection) args._)))
(def result* (str (JSON.stringify result nil 2) "\n"))

(if (= args.output "-")
  (print result*)
  (fs.write-file-sync args.output result*))
