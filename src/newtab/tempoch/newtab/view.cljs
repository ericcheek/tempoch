(ns tempoch.newtab.view
  (:require
   [reagent.core :as reagent]
   [chromex.logging :refer-macros [log info warn error group group-end]]
   [tempoch.newtab.actions :as actions]))

(defn tab-view [ctx tab-info]
  (log (clj->js tab-info))
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

(defn window-view [ctx window]
  (into 
   [:div {:class (str
                  "window-group"
                  (if (:focused window) " active-window"))
          :key (:id window)}]
   (map (partial tab-view ctx) (:tabs window))))

(defn app-view [ctx]
  (let
      [tabs-by-window (->> @ctx :bg-state :windows)]
    [:div {:class "top-flex"}
     (map (partial window-view ctx) tabs-by-window)]))


(defn render [ctx]
  (reagent/render
   [app-view ctx]
   (.getElementById js/document "app")))


  
