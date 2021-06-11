(ns app.view.components
  (:require ["@blueprintjs/core" :as bp]
            [clojure.string :as s]
            [medley.core :refer [indexed]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [app.macros :refer-macros [defwrapper defwrapper*]]
            [app.util :refer [*value merge* favicon-prefix splice ->vector]]
            [app.util.blueprint :refer [class]]
            [app.fx :refer [open-file]]))

(defn loading []
  [:> bp/Spinner {:class :loading, :intent :primary, :size 100}])

(def input-class  [:bp3-fill (:input class)])

(defn favicon [url]
  (if-let [icon @(rf/subscribe [:favicon-url url])]
    [:img.icon {:src icon}]))

(defn set-href [s] (rf/dispatch [:set-href s]))
(defn href [url & content]
  (let [blank @(rf/subscribe [:to-blank])]
    (into [:a {:href url, :target (if blank :_blank), :on-mouse-enter #(set-href url), :on-mouse-leave #(set-href nil)}
            [favicon url]]
          content)))

(defwrapper icon-button  bp/Button (merge* attrs {:class :bp3-minimal}))
(defwrapper input-button bp/Button (merge* attrs {:class (:input class)}))
(defwrapper tag          bp/Tag    (merge* attrs {:minimal true}))
(defwrapper textarea     :textarea (merge* {:rows 5} attrs))
(defwrapper tag-input    bp/TagInput
  (let [private? @(rf/subscribe [:private-tags])]
    (merge* {:add-on-blur true,  :separator ",",  :tag-props #(clj->js {:minimal true, :intent (if (private? %) :danger)})}
            attrs)))
(defwrapper* dropdown [attrs & content]
  [:div.bp3-fill {:class (:select class)}
    (into [:select (dissoc attrs :class :label :type)] content)])

(defwrapper* input [form field attrs & content]
  (let [type (case (:type attrs)
               :tags     tag-input
               :select   dropdown
               :textarea textarea
               :input)]
    [:> bp/Label {:on-click #(.preventDefault %)}               ; TODO: form group?
       [:b (:label attrs)]
       [:div.row
         (into [type (merge* {:class input-class, :on-change (*value [:set-in [form field]])} attrs)]
               content)
         (:after attrs)]]))

(defn list-input [form field attrs]
  (let [value  (->vector (or (:value attrs) [""]))
        input* (fn [i s] [:input (merge* attrs {:class input-class, :value s, :on-change (*value [:set-in [form field i]])})])]
    [:> bp/Label
      [:b (:label attrs)]
      (into [:<>]
        (for [[i s] (indexed value), :let [last? (= (inc i) (count value))]] ^{:key i}
          [:div.row (input* i s)
                    [icon-button (if last?
                                   {:icon :add, :on-click #(rf/dispatch [:set-in [form field] (conj value "")])}
                                   {:icon :remove, :intent :danger,
                                    :on-click #(rf/dispatch [:set-in [form field] (splice value i)])})]]))]))

(defn image-input [form field {:keys [id label value uploaded]}]
  [:> bp/Label
    [:b label]
    (cond (not uploaded) [:div.row
                            [:input {:class input-class, :value value, :on-change (*value [:set-in [form field]])}]
                            (if-not (seq value)
                              [icon-button {:icon :upload,  :title "Upload"
                                            :on-click #(open-file "image/*" [:upload-image form field id])}])]
          (seq value)    [:> bp/Button {:class :float-right, :icon :trash, :intent :danger,
                                        :on-click #(rf/dispatch [:remove-image form field id])} "Remove"]
          :else          [:> bp/Spinner])
    (if (seq value) [:div [:br] [:img.poster {:src value}]])])

(defn favicon-input [form field {:keys [label url value update]}]
  [:> bp/Label
    [:b label " (" (favicon-prefix url) "/…)"]
    [:div.row
      [:span.input-prefix [favicon url]]
      [:input {:class input-class, :value value :on-change (*value [:set-in [form field]])}]
      [icon-button {:icon :refresh, :on-click #(update (favicon-prefix url) value)}]]])
