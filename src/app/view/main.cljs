(ns app.view.main
  (:require ["@blueprintjs/core" :as bp]
            [clojure.string :as s]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [app.api :refer [root]]
            [app.util :refer [title title*]]
            [app.util.blueprint :refer [align boundary class]]
            [app.fx :refer [toggle-fullscreen]]
            [app.view.urls :refer [breadcrumbs url edit-dialog]]
            [app.view.tags :refer [unlocked-tags tags-dialog]]
            [app.view.dialog :refer [confirm-dialog]]
            [app.view.components :as comp]))

(defn header []
  (let [user @(rf/subscribe [:user])]
    [:> bp/Navbar {:class :row, :fixed-to-top true}
      [:> bp/NavbarGroup {:class [:grow :shrink]}; :style {:overflow-x :hidden}}
        [comp/icon-button {:intent :success, :icon :add, :on-click #(rf/dispatch [:new-url])}]
        [unlocked-tags]]
      [:> bp/NavbarGroup {:class :shrinkX, :align (:right align)}
        [comp/icon-button {:icon :fullscreen, :on-click toggle-fullscreen}]
        [:> bp/Text {:ellipsize true} user]
        [comp/icon-button {:intent :primary, :icon :log-out, :on-click #(rf/dispatch [:logout])}]]]))

(defn node-header [node]
  (let [[node cache] (for [k [:node :cache]] @(rf/subscribe [k]))]
    (if-not (= node root)
      [:<>
        [breadcrumbs @(rf/subscribe [:path])]
        [url node (get cache node) :expanded true :path-tags @(rf/subscribe [:path-tags])]])))

(defn urls []
  [:main {:role :main} [node-header]
    (into [:div (if @(rf/subscribe [:path-locked?]) {:class (:skeleton class)})]
      (for [[k v] (sort-by title* @(rf/subscribe [:urls]))]
        ^{:key k} [url k v]))])

(defn qrcode []
  (if-let [url @(rf/subscribe [:href])]
    [:img.qrcode {:src url}]))

(defn scroll-top []
  (when-not (zero? @(rf/subscribe [:scroll]))
    [comp/icon-button {:class :bottom,  :icon :arrow-up,  :intent :primary
                       :title "Scroll to top",  :on-click #(rf/dispatch [:scroll :top])}]))

(defn main []
  [:<>
    [header]
    (and @(rf/subscribe [:path]) [urls])
    [edit-dialog]
    [confirm-dialog]
    [tags-dialog]
    [qrcode]
    [scroll-top]])
