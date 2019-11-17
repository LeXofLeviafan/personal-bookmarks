(ns app.view.tags
  (:require ["@blueprintjs/core" :as bp]
            [oops.core :refer [oapply]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [app.util.blueprint :refer [boundary]]
            [app.view.dialog :refer [dialog]]
            [app.view.components :as comp]))

(defn- unlocked-tag [item]
  [:div.header-tag
    (if (string? item)
      [comp/tag item]
      [:> bp/Button {:on-click (:on-click item)} (:button item)])])

(defn unlocked-tags []
  [:> bp/OverflowList
    {:items                 (oapply #js[{:button "Tags", :on-click #(rf/dispatch [:tags-dialog])}]
                                    :concat (sort @(rf/subscribe [:unlocked-tags])))
     :min-visible-items     1
     :collapse-from         (:end boundary)
     :observe-parents       true
     :visible-item-renderer #(r/as-element ^{:key %2} [unlocked-tag %1])
     :overflow-renderer     #(r/as-element [comp/icon-button {:icon :more}])}])

(defn tags-dialog []
  [dialog :tags-form
    {:close-event [:close-tags-dialog]
     :body (fn [{:keys [tag passphrase error]}]
             (let [unlock        #(rf/dispatch [:add-passphrase passphrase {}])
                   [bind unbind] (for [k [:create-tag :remove-tag]] #(rf/dispatch [k tag passphrase {}]))
                   to-fill       (not (and (seq passphrase) (seq tag)))]
               [:<>
                 (into [:div.taglist] (for [s (sort @(rf/subscribe [:unlocked-tags]))]
                                         [comp/tag {:intent :danger, :on-remove #(rf/dispatch [:lock-tag s])} s]))
                 [comp/input :tags-form :passphrase
                             {:value passphrase, :label "Passphrase", :type :password
                              :after [comp/input-button {:icon :unlock, :on-click unlock, :disabled (not (seq passphrase))}
                                       "Unlock"]}]
                 [comp/input :tags-form :tag
                             {:value tag, :label "Tag"
                              :after [:<> [comp/input-button {:icon :key,   :on-click bind,   :disabled to-fill}   "Bind"]
                                          [comp/input-button {:icon :trash, :on-click unbind, :disabled to-fill} "Unbind"]]}]
                 (and error [:> bp/Callout {:intent :warning} error])]))}])
