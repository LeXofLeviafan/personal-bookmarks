(ns app.view.urls
  (:require ["@blueprintjs/core" :as bp]
            ["marked" :as marked]
            [oops.core :refer [ocall]]
            [medley.core :refer [indexed]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [app.api :refer [root]]
            [app.util :refer [*html title title* show-date fix-url updated details get-path]]
            [app.util.blueprint :refer [class]]
            [app.view.dialog :refer [dialog]]
            [app.view.components :as comp]))

(def allow-drop #(.preventDefault %))

(def drag       #(let [id (ocall % [:currentTarget :getAttribute] "itemid")]
                   (ocall % [:dataTransfer :setData] "itemid" id)))

(def drop*      #(let [id (ocall % [:dataTransfer :getData] "itemid")
                       to (ocall % [:currentTarget :getAttribute] "itemid")]
                   (rf/dispatch [:move-url id to])
                   (.preventDefault %)))

(def skeleton (:skeleton class))

(def markdown-options #js{:headerIds   false  ; generate headers without an id attribute
                          :smartLists  true   ; separate same-level lists with different bullets (*/-/+)
                          :smartypants true}) ; convert quotes ("/'), dashes (--/---) and ellipsis (...)


(letfn [(render-item [locked? {:keys [id url] :as item}]
          [:div {:itemID id, :on-drag-over allow-drop, :on-drop drop*, :title (details item)}
             (if (not= id root)
               [:span (if locked? {:class skeleton}) [comp/favicon url] (or (title item) id)]
               [:span [:> bp/Icon {:icon :home}] " Home"])])
        (convert [items locked-index]
          (-> (for [[i item] (indexed items)]
                #js{:onClick #(rf/dispatch [:change-list (:id item)])
                    :text    (r/as-element [render-item (>= i locked-index) item])})
              js/Array.from))]
  (defn breadcrumbs [path]
    (if-not path
      [:> bp/Spinner {:intent :primary}]
      [:> bp/Breadcrumbs {:class :breadcrumbs, :items (convert path @(rf/subscribe [:first-locked]))}])))


(defn url [id {:keys [created url image description status progress bookmark tags links] :as data}
           & {:keys [expanded path-tags]}]
  [:> bp/Card (if expanded
                {:class (if @(rf/subscribe [:path-locked?]) skeleton :background)}
                {:class :mini, :title (details data), :itemID id, :interactive true, :on-drag-over allow-drop, :on-drop drop*})
    [:div.float-right
      [comp/icon-button {:icon :edit, :on-click #(rf/dispatch [:edit-url id data])}]
      (if (and expanded (empty? @(rf/subscribe [:data])))
        [comp/icon-button {:icon :remove, :on-click #(rf/dispatch [:del-url id]), :intent :danger}])]
    [:h6.url
      (if-not expanded [comp/icon-button {:icon :caret-right, :on-click #(rf/dispatch [:change-list id])}])
      [:span (if-not expanded {:itemID id, :draggable true, :on-drag-start drag})
        (if-not (seq url) (title data) [comp/href url (title data)])]
      (if-not (empty? status) [comp/tag {:intent :warning} status])
      (if-not (empty? (str bookmark progress))
        [comp/tag {:intent :primary} [comp/href bookmark progress]])
      (let [private? @(rf/subscribe [:private-tags])]
        (for [s (or path-tags tags)] ^{:key s} [comp/tag (if (private? s) {:intent :danger}) s]))]
    (when expanded
      [:div.details
        (and image [:img.poster {:src image}])
        [:div (-> (or description "") (marked markdown-options) *html)]
        (when links
          [:ul
            [:b "Links:"]
            (into [:<>] (for [s links] [:li [comp/href s s]]))])
        [comp/tag {:intent :success} "added " (show-date created)]
        (when-let [x (updated data)]
          [comp/tag {:intent :success} "updated " (show-date x)])])])


(defn edit-dialog []
  [dialog :url-form
    {:close-event [:cancel-url-edit], :confirm-event [:save-url-changes]
     :body (fn [{:keys [id name url tags image description status progress bookmark links parent] :or {tags []} :as data}]
             (let [[node items cache] (for [k [:node :urls :cache]] @(rf/subscribe [k]))
                   items*             (->> (sort-by title* items)
                                           (map (fn [[k v]] (assoc v :id k)))
                                           (concat (get-path node cache)))]
               [:<>
                 [comp/input :url-form :url         {:value  url,            :label "URL",         :type :url}]
                 (when (seq url)
                   [comp/favicon-input :url-form :favicon
                     {:label "Favicon URI", :url (fix-url url),
                      :value (or (:favicon data) @(rf/subscribe [:favicon (fix-url url)])),
                      :update #(rf/dispatch (into [:change-favicon] %&))}])
                 [comp/input :url-form :name        {:value  name,           :label "Name"}]
                 [comp/image-input :url-form :image {:value  image,          :label "Image (URL)"
                                                     :id     id,             :uploaded (:image-uploaded data)}]
                 [comp/input :url-form :tags        {:values (clj->js tags), :label "Tags",        :type :tags}]
                 [comp/input :url-form :description {:value  description,    :label "Description", :type :textarea}]
                 [comp/input :url-form :status      {:value  status,         :label "Status"}]
                 [comp/input :url-form :progress    {:value  progress,       :label "Progress"}]
                 [comp/input :url-form :bookmark    {:value  bookmark,       :label "Bookmark",    :type :url}]
                 [comp/list-input :url-form :links  {:value  links,          :label "Links",       :type :url}]
                 [comp/input :url-form :parent      {:value  parent,         :label "Parent",      :type :select}
                   (for [x items*, :when (not (#{(:id x) (:parent x)} id))]
                     ^{:key (:id x)} [:option {:value (:id x)} (or (title x) "—")])]]))}])
