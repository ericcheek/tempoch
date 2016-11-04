(ns tempoch.newtab.view
  (:require
   [reagent.core :as reagent]
   [tempoch.newtab.actions :as actions]))

(defn tab-view [ctx tab-info]
  [:div {:key (:id tab-info)
         :class (str
                 "tab-view"
                 (if (:highlighted tab-info) " highlighted"))
         :on-click (fn [e]
                     (actions/activate-tab
                      (:bg-port @ctx)
                      tab-info))}
   [:span {}
    (:title tab-info)]])

(defn window-view [ctx [window-id tabs]]
  (into 
   [:div {:class "window-group" :key window-id}]
   (map (partial tab-view ctx) tabs)))

(defn app-view [ctx]
  (let
      [tabs-by-window (->>
                       @ctx
                       :bg-state
                       :tabs
                       (group-by :windowId))]
    [:div {:class "top-flex"}
     (map (partial window-view ctx) tabs-by-window)]))


(defn render [ctx]
  (reagent/render
   [app-view ctx]
   (.getElementById js/document "app")))


  
