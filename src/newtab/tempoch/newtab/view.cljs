(ns tempoch.newtab.view
  (:require
   [chromex.logging :refer-macros [log info warn error group group-end]]
   [reagent.core :as reagent]
   [tempoch.newtab.actions :as actions]
   [tempoch.newtab.util :refer [classes]]))
        

(defn fa-icon [name]
  [:i {:class (str "fa fa-" name) :aria-hidden true}])

(defn icon-button [icon action-fn & params]
  [:button
   {:on-click
    (fn [e]
      (.stopPropagation e)
      (apply action-fn params))}
   (fa-icon icon)])

(defn tab-actions [tab]
  [:div {:class "tab-actions"}
   [:a {:href (:url tab)
       :on-click (fn [e] (.stopPropagation e))}
    (fa-icon "link")]
   (icon-button "close" actions/close-tab! tab)])

(defn tab-view [tab]
   [:div {:key (:id tab)
          :class (classes
                  "tab-view"
                  [(:active tab) "active-tab"])
          :title (:title tab)
          :on-click (fn [e]
                      (.stopPropagation e)
                      (actions/activate-tab! tab))}
    [:div
     (cond
       (-> tab :mutedInfo :muted) (fa-icon "volume-off")
       (-> tab :audible) (fa-icon "volume-up")
       :default nil)
     (if (-> tab :incognito) (fa-icon "user-secret"))
     [:span {:class (classes [(:incognito tab) "incognito"])}
      (:title tab)]]
    (tab-actions tab)])

(defn new-tab-input [window]
  (let [vatom (reagent/atom "")]
    (fn [window]
      [:input {:type "text"
               :class "new-tab-input"
               :on-change #(reset! vatom (-> % .-target .-value))
               
               :value @vatom
           
               :on-key-press
               (fn [e]
                 (when (= (.-charCode e) 13)
                   (actions/open-tab!
                    window
                    (-> e .-target .-value)
                    (not (.-shiftKey e)))
                   (reset! vatom "")))
               }])))


(defn window-actions [window]
  (let
      [button (fn [icon action-fn]
                (icon-button icon action-fn window))]

    [:div {:class "window-actions"}
     (if (-> window :state (= "minimized"))
       (button "window-restore" actions/show-window!)
       (button "window-minimize" actions/minimize-window!))
     (button "close" actions/close-window!)]))


(defn window-view [window]
  [:div {:class (classes "window-view"
                         [(:focused window) "active-window"])
         :key (:id window)}
   [:div {:class "window-title"}
    (if (:incognito window) [:span {:style {:color "red"}} "incognito"])
    (if (-> window :type (= "devtools")) [:span {:style {:color "green"}} "devtools"])]
   (window-actions window)
   (map tab-view (:tabs window))
   [new-tab-input window]
   ])



(defn search-box [ctx]
  [:div {:class "search-box"}
   [:input {:type "text"}]])

(defn app-view [ctx]
  (let
      [tabs-by-window (->> @ctx :bg-state :windows)]
    [:div
     ;;(search-box ctx)
     [:div {:class "top-flex"}
      (map window-view tabs-by-window)]]))


(defn render [ctx]
  (reagent/render
   [app-view ctx]
   (.getElementById js/document "app")))
