(ns tempoch.newtab.view
  (:require
   [chromex.logging :refer-macros [log info warn error group group-end]]
   [reagent.core :as reagent]
   [tempoch.common.macros :refer-macros [evfn]]
   [tempoch.newtab.actions :as actions]
   [tempoch.newtab.state :as state]
   [tempoch.newtab.util :refer [classes] :as util]))


(defn fa-icon [name]
  [:i {:class (str "fa fa-" name) :aria-hidden true}])

(defn icon-button-e [icon action-fn & params]
  [:button
   {:on-click
    (evfn ^:stop [e]
      (apply action-fn e params))}
   (fa-icon icon)])

(defn icon-button [icon action-fn & params]
  (apply icon-button-e
         icon
         (fn [e & args] (apply action-fn args))
         params))

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

(defn drop-zone [& _]
  (let [activated (reagent/atom false)]
    (fn [{:keys [drop-fn drop-class tag] :or {tag :div}} & [child :as children]]
      (if (util/multiple? children) (error "Only 1 child supported"))
      [tag
       {:on-drag-enter
        (evfn [e]
          (reset! activated true)
          (state/set-drop-fn! drop-fn))

        :on-drag-over
        (evfn ^:prevent [e])

        :on-drag-leave
        (evfn [e]
          (reset! activated false))

        :on-drop
        (evfn [e]
          (let [{:keys [drop-fn selection]} (state/get-drag-state)]
            (when drop-fn
              (drop-fn selection))
            (reset! activated false)))

        :class (classes "drop-zone" [@activated drop-class])}
       child])))

(defn drag-source [& _]
  (fn [{:keys [selection tag] :or {tag :div}} & [child :as children]]
    (if (util/multiple? children) (error "Only 1 child supported"))
    [tag
     {:draggable true

      :on-drag-start
      (evfn [e]
        (state/set-drag-selection! selection))

      :on-drag-end
      (evfn [e]
        (state/clear-drag-state!))}
     child]))

(defn tab-actions [tab]
  [:div {:class "tab-actions"}
   [:a {:href (:url tab)
        :on-click (evfn ^:stop [e])}
    (fa-icon "link")]
   (icon-button "close" actions/close-tab! tab)])

(defn tab-audio-control [tab]
  (let
      [click-handler
       (evfn [e should-mute]
         (when (.-shiftKey e)
           (actions/mute-other-tabs! tab))
         (actions/set-tab-mute! tab should-mute))]
    (cond
      (-> tab :mutedInfo :muted)
      (icon-button-e
       "volume-off"
       click-handler false)

      (-> tab :audible)
      (icon-button-e
       "volume-up"
       click-handler true)

      :default nil)))

(defn tab-view [tab]
  (let [tab-state
        (reagent/atom {:editing false})]
    (fn [tab]
      [:div
       {:class (classes
                "tab-view"
                [(:active tab) "active-tab"]
                [(state/is-tab-selected? tab) "selected-tab"])
        :title (:title tab)
        :on-click
        (evfn ^:prevent [e]
          (cond
            (:editing @tab-state) nil
            (.-shiftKey e) (swap! tab-state assoc :editing true)
            (.-metaKey e) (if (state/is-tab-selected? tab)
                              (state/remove-from-selection! tab)
                              (state/add-to-selection! tab))
            :default (actions/activate-tab! tab)))
        }
       [:div
        [tab-audio-control tab]
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
                 (if (pos? (count @value)) @value)
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
     [drag-source
      {:selection (map :id (:tabs window)) :tag :button}
      (fa-icon "list-alt")]
     (if (should-mask window)
       (button "eye" actions/set-window-masked! false)
       (button "eye-slash" actions/set-window-masked! true))
     (if (-> window :state (= "minimized"))
       (button "window-restore" actions/show-window!)
       (button "window-minimize" actions/minimize-window!))
     (button "close" actions/close-window!)]))

(defn window-view [window {vis-forced :force-visible}]
  (let
      [is-devtools (-> window :type (= "devtools"))]
    [:div {:class (classes "window-view"
                           [(:focused window) "active-window"]
                           [(and
                             (not vis-forced)
                             (should-mask window)) "masked-details"])
           :key (:id window)}

     [:div {:class "window-title"}
      (if (:incognito window) [:span {:style {:color "#dc322f"}} "incognito"])
      (if is-devtools [:span {:style {:color "green"}} "devtools"])]
     (window-actions window)
     (if is-devtools
       [tab-view (-> window :tabs first)])

     (if (not is-devtools)
       (->>
        (:tabs window)
        (util/mapall
         (fn [tab]
           ^{:key (:id tab)}
           [drop-zone
            {:drop-fn (partial actions/move-tabs! window (:index tab))
             :drop-class "tab-drop-ready"
             :on-drop state/clear-selection!}
            [drag-source
             {:selection
              (conj (state/get-tab-selection) (:id tab))}
             [tab-view tab]]]))))
     (if (not is-devtools)
       [drop-zone
        {:drop-fn (partial actions/move-tabs! window -1)
         :drop-class "tab-drop-ready"}
        [new-tab-input {:window window}]])]))

(defn search-box [ctx]
  [:div {:class "search-box"}
   [:input {:type "text"}]])

(defn create-window-button [is-incognito]
  [drop-zone
   {:drop-fn (partial actions/open-window-with-tabs! is-incognito)
    :drop-class "tab-drop-ready"
    :tag :span}
   [:button
    {:on-click (evfn [e]
                 (actions/open-window!
                  (-> e .-shiftKey not)
                  is-incognito))}
    (if is-incognito
      "+incognito" "+window")]])

(defn app-view [_]
  (let
      [force-visibile-windows (reagent/atom nil)]
    (fn [ctx]
      (let
          [chrome-windows (-> (state/get-chrome) :windows)
           persistent-windows (-> (state/get-persistent) :windows)
           windows (->>
                    chrome-windows
                    (map (fn [[k w]]
                           (merge w (get persistent-windows k))))
                    (sort-by #(vector (-> % :type (= "normal")) (:id %)))
                    reverse)]
      [:div
       {:class (classes [(state/is-drag-active?) "drag-active"])}
       ;;(search-box ctx)
       [:div
        [create-window-button false]
        [create-window-button true]
        (if @force-visibile-windows
          [icon-button "low-vision" #(reset! force-visibile-windows nil)]
          [icon-button "eye" #(reset! force-visibile-windows true)])]
       [:div {:class "top-flex"}
        (util/mapall
         (fn [w] (window-view w {:force-visible @force-visibile-windows}))
         windows)]]))))

(defn render [ctx]
  (log (js/window.performance.now))
  (reagent/render
   [app-view ctx]
   (.getElementById js/document "app")))
