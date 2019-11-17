(ns app.view.dialog
  (:require ["@blueprintjs/core" :as bp]
            [re-frame.core :as rf]
            [app.util :refer [force-evt]]
            [app.util.blueprint :refer [class]]))

(defn dialog [sub {:keys [close-event confirm-event body]}]
  (when-let [data @(rf/subscribe [sub])]
    (let [close  #(rf/dispatch close-event)
          action (if (fn? confirm-event) (confirm-event data) (or confirm-event close-event))]
      [:> bp/Dialog {:is-open true, :on-close close, :can-outside-click-close false}
        [:form.dialog.col
          [:div.shrink {:class (:dialog-body class)}
            [body data]]
          [:div {:class (:dialog-footer class)}
            [:div {:class (:dialog-footer-actions class)}
              (if-not (identical? action close-event) [:> bp/Button {:on-click close} "Cancel"])
              [:> bp/Button {:on-click (force-evt action), :intent :primary, :type :submit} "OK"]]]]])))

(defn confirm-dialog []
  [dialog :confirmation {:close-event   [:set :confirmation nil]
                         :confirm-event (fn [[_ action]] action)
                         :body          (fn [[confirmation _]] confirmation)}])
