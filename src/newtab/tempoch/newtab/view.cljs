(ns tempoch.newtab.view
  (:require
   [chromex.logging :refer-macros [log info warn error group group-end]]
   [reagent.core :as reagent]
   [tempoch.newtab.actions :as actions]
   [tempoch.newtab.state :as state]
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

(defn tab-nav-input [{:keys [on-enter classes initial-value]}]
  (let [value (reagent/atom (or initial-value ""))]
    (fn [window]
      [:input
       {:type "text"
        :class classes
        :on-change #(reset! value (-> % .-target .-value))

        :value @value

        :on-key-press
        (fn [e]
          (when (= (.-charCode e) 13)
            (on-enter e value)))}])))

(defn drop-zone [{:keys [drop-fn drop-class]}]
  (let [activated (reagent/atom false)]
    (fn [_ children]
      [:div.drop-zone
       {:on-drag-enter
        (fn [e]
          (reset! activated true)
          (swap! state/local-ctx assoc-in [:drag-state :drop-fn] drop-fn))
        
        :on-drag-over
        (fn [e] (.preventDefault e))
        
        :on-drag-leave
        (fn [e]
          (reset! activated false))
        
        :on-drop (fn [e]
                   (let [{:keys [drop-fn selection]} (get @state/local-ctx :drag-state)]
                     (when drop-fn
                       (drop-fn selection))
                     (reset! activated false)))
        :class (classes [@activated drop-class])}
       children])))

(defn drag-source [{:keys [selection]}]
  (fn [_ children]
    [:div
     {:draggable true
      :on-drag-start
      (fn [e]
        (swap! state/local-ctx assoc :drag-state {:selection selection}))
      :on-drag-end
      (fn [e]
        (swap! state/local-ctx assoc :drag-state nil))}
     children]))

(defn tab-actions [tab]
  [:div {:class "tab-actions"}
   [:a {:href (:url tab)
       :on-click (fn [e] (.stopPropagation e))}
    (fa-icon "link")]
   (icon-button "close" actions/close-tab! tab)])

(defn tab-view [tab]
  (let [tab-state
        (reagent/atom {:editing false})]
    (fn [tab]
      [:div
       {:class (classes
                "tab-view"
                [(:active tab) "active-tab"])
        :title (:title tab)
        :on-click
        (fn [e]
          (cond
            (:editing @tab-state) nil
            (.-shiftKey e) (do
                             (.stopPropagation e)
                             (swap! tab-state assoc :editing true))
            :default (actions/activate-tab! tab)))
        }
       [:div
        (cond
          (-> tab :mutedInfo :muted) (fa-icon "volume-off")
          (-> tab :audible) (fa-icon "volume-up")
          :default nil)
        (if (-> tab :incognito) (fa-icon "user-secret"))
        (if (-> @tab-state :editing)
          [tab-nav-input
           {:on-enter (fn [e value]
                        (when (not= @value (:url tab))
                          (actions/navigate-tab! tab @value))
                        (swap! tab-state assoc :editing false))
            :initial-value (:url tab)}]
          [:span {:class (classes "tab-title" [(:incognito tab) "incognito"])}
           (:title tab)])]
       (tab-actions tab)])))



(defn new-tab-input [{:keys [window]}]
  [tab-nav-input
   {:on-enter (fn [e value]
                (actions/open-tab!
                 window
                 @value
                 (not (.-shiftKey e)))
                (reset! value ""))
    :classes "new-tab-input"}])

(defn should-mask [window]
  (-> window (:masked (:incognito window))))

(defn window-actions [window]
  (let
      [button (fn [icon action-fn & params]
                (apply
                 icon-button icon action-fn window params))]

    [:div {:class "window-actions"}
     (if (should-mask window)
       (button "eye" actions/set-window-masked! false)
       (button "eye-slash" actions/set-window-masked! true))
     (if (-> window :state (= "minimized"))
       (button "window-restore" actions/show-window!)
       (button "window-minimize" actions/minimize-window!))
     (button "close" actions/close-window!)]))



(defn window-view [window]
  (let
      [is-devtools (-> window :type (= "devtools"))]
    [:div {:class (classes "window-view"
                           [(:focused window) "active-window"]
                           [(should-mask window) "masked-details"])
           :key (:id window)}
     [:div {:class "window-title"}
      (if (:incognito window) [:span {:style {:color "red"}} "incognito"])
      (if is-devtools [:span {:style {:color "green"}} "devtools"])]
     (window-actions window)
     (if is-devtools
       [tab-view (-> window :tabs first)] ;; assuming one tab for now       
       (map
        (fn [tab] 
          ^{:key (:id tab)}
          [drop-zone
           {:drop-fn (partial actions/move-tabs! window (:index tab))
            :drop-class "tab-drop-ready"}
           [drag-source {:selection (:id tab)}
            [tab-view tab]]])
        (:tabs window)))
      (if (not is-devtools)
        [drop-zone
         {:drop-fn (partial actions/move-tabs! window -1)
          :drop-class "tab-drop-ready"}
         [new-tab-input {:window window}]])]))


(defn search-box [ctx]
  [:div {:class "search-box"}
   [:input {:type "text"}]])


(defn create-window-button [is-incognito]
  [:button
   {:on-click (fn [e]
                (actions/open-window!
                 (-> e .-shiftKey not)
                 is-incognito))}
   (if is-incognito
     "+incognito" "+window")])


(defn app-view [ctx]
  (let
      [chrome-windows (->> @ctx :bg-state :chrome :windows)
       transient-windows (->> @ctx :bg-state :transient :windows)
       windows (->>
                chrome-windows
                (map (fn [[k w]]
                       (merge w (get transient-windows k))))
                (sort-by #(vector (-> % :type (= "normal")) (:id %)))
                reverse)]
   
    [:div
     {:class (classes [(:drag-state @state/local-ctx) "drag-active"])}
     ;;(search-box ctx)
     [:div
      [create-window-button false]
      [create-window-button true]]
     [:div {:class "top-flex"}
      (map window-view windows)]]))


(defn render [ctx]
  (reagent/render
   [app-view ctx]
   (.getElementById js/document "app")))
