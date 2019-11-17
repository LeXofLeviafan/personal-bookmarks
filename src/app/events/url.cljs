(ns app.events.url
  (:require [medley.core :refer [map-keys]]
            [app.macros :refer-macros [defvalidator defevent]]
            [app.api :refer [fields urls sets root]]
            [app.util :refer [->key title fix-urls fix-sets add-dates]]))

(defvalidator nil)


(defevent :db :new-url [db _]
  (assoc db :url-form {:parent (:node db)}))

(defevent :db :edit-url [db [_ id data]]
  (assoc db :url-form (merge data {:id id})))

(defevent :db :cancel-url-edit [db _]
  (assoc db :url-form nil))

(defevent :db :del-url [db [_ id]]
  (if (seq id)
    (let [name (title (get-in db [:cache id]))]
      (assoc db :confirmation [(str "Remove \"" name "\"?") [:-del-url id]]))))
(defevent :fx :-del-url [{db :db} [_ id]]
  (if (seq id)
    {:db     (assoc db :confirmation nil)
     :fb-del (str "/urls/" (->key id))
     :emit   (and (= id (:node db)) [[:change-list (get-in db [:cache id :parent])]])}))


(defevent :fx :-save-url [_ [_ id data]]
  (if (not= id "")
    (let [m (->> data add-dates (fix-urls urls) (fix-sets sets))]
      {:fb-save      {:uri "/urls", :id id, :data (clj->js (select-keys m fields))}
       :remove-files {:paths (:remove data)}})))

(defevent :fx :save-url-changes [{db :db} _]
  (let [data (:url-form db)]
    {:db   (assoc db :url-form nil)
     :emit [[:-save-url (:id data) data]]}))

(defevent :fx :move-url [{db :db} [_ id to]]
  (if (and to (seq id) (not= id to))
    (let [x (get-in db [:cache id])]
      {:emit [[:-save-url id (assoc x :parent to)]]})))

(defevent :fx :fix-url-parent [_ [_ id value]]
  (if-not (:parent value)
    {:emit [[:-save-url id (assoc value :parent root)]]}))


(defevent :fx :fetch-url [{db :db} [_ id]]
  (let [x (get-in db [:cache id])]
    (cond (nil? id)   {}
          (= id root) {}
          x           {:emit     [[:fetch-url (:parent x)]]}
          :else       {:fb-fetch {:uri        (str "/urls/" (->key id))
                                  :on-success [:cache-url id]}})))

(defevent :fx :watch-node [{{:keys [node node-ref]} :db} _]
  (if-not (or node-ref (= node root))
    {:fb-mksub {:uri        (str "/urls/" (->key node))
                :key        :node-ref
                :on-success [:cache-url node]
                :on-failure [:change-list root]}}))

(defevent :fx :node-unsub [{db :db} _]
  {:db       (dissoc db :node-ref)
   :fb-unsub (:node-ref db)})

(defevent :fx :cache-url [{db :db} [_ id value]]
  (let [v (map-keys keyword value)
        x (assoc v :parent (or (:parent v) root))]
    {:db    (assoc db :cache (assoc (:cache db) id x))
     :emit  [[:fetch-url (:parent x)]
             [:fix-url-parent id v]]}))


(defn- upload* [field] (keyword (str (name field) "-uploaded")))

(defevent :fx :upload-image [{db :db} [_ form field id file]]
  (let [path (str "images/" id)]
    {:db          (update db form merge {(upload* field) true, :remove (disj (get-in db [form :remove]) path)})
     :upload-file {:path path, :file file, :on-success [:set-in [form field]]}}))

(defevent :db :remove-image [db [_ form field id]]
  (let [removed (set (get-in db [form :remove]))]
    (update db form merge {field nil, (upload* field) nil, :remove (conj removed (str "images/" id))})))
