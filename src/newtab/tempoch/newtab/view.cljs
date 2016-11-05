(ns tempoch.newtab.view
  (:require
   [reagent.core :as reagent]
   [chromex.logging :refer-macros [log info warn error group group-end]]
   [tempoch.newtab.actions :as actions]))


(defn fa-icon [name]
  [:i {:class (str "fa fa-" name) :aria-hidden true}])


(defn tab-view [tab-info]
   [:div {:key (:id tab-info)
          :class (str
                  "tab-view"
                  (if (:active tab-info) " active-tab"))
          :title (:title tab-info)
          :on-click (fn [e]
                      (.stopPropagation e)
                      (actions/activate-tab! tab-info))}
    [:div
     (cond
       (-> tab-info :mutedInfo :muted) (fa-icon "volume-off")
       (-> tab-info :audible) (fa-icon "volume-up")
       :default nil)
     (if (-> tab-info :incognito) (fa-icon "user-secret"))
     [:span {:class (str ""
                         (if (:incognito tab-info) " incognito"))}
      (:title tab-info)]]])


(defn window-actions [window]
  (let
      [button (fn [icon action-fn]
                [:button
                 {:on-click
                  (fn [e]
                    (.stopPropagation e)
                    (action-fn (:id window)))}
                 (fa-icon icon)])]
                
    [:div {:class "window-actions"}
     (if (-> window :state (= "minimized"))
       (button "window-restore" actions/show-window!))
     (if (-> window :state (not= "minimized"))
       (button "window-minimize" actions/minimize-window!))
     (button "close" actions/close-window!)]))


(defn window-view [window]
  (->
   [:div {:class (str
                  "window-group"
                  (if (:focused window) " active-window"))
          :key (:id window)
          :on-click #(actions/show-window! (:id window))}
    (if (:incognito window) [:span {:style {:color "red"}} "incognito"])
    (if (-> window :type (= "devtools")) [:span {:style {:color "green"}} "devtools"])
    (window-actions window)]
   (into (map tab-view (:tabs window)))))


(defn app-view [ctx]
  (let
      [tabs-by-window (->> @ctx :bg-state :windows)]
    [:div {:class "top-flex"}
     (map window-view tabs-by-window)]))


(defn render [ctx]
  (reagent/render
   [app-view ctx]
   (.getElementById js/document "app")))
