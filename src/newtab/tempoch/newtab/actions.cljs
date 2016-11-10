(ns tempoch.newtab.actions
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols :refer [post-message!]]
            [tempoch.newtab.state :as state]))

(defn send-action-legacy! [action params]
  (post-message!
   (:bg-port @state/app-ctx)
   (clj->js
    {:action action
     :params params})))

(defn send-action! [& action-specs]
  (send-action-legacy!
   "passthrough"
   (prn-str (into [] action-specs))))

    
(defn activate-tab! [tab]
  (send-action!
   [:tabs/update (:id tab) {:active true}]
   [:windows/update (:windowId tab) {:focused true}]))

(defn navigate-tab! [tab query]
  (send-action-legacy! "navigate-tab"
                {:tab-id (:id tab)
                 :query query}))

(defn close-tab! [tab]
  (send-action-legacy! "close-tab"
                {:tab-id (:id tab)}))

(defn open-tab! [window query switch-to]
  (send-action-legacy! "open-tab"
                {:window-id (:id window)
                 :query query
                 :active switch-to}))

(defn move-tabs! [& [window index tab-ids :as args]]
  (state/clear-selection!) ;; feels like the wrong place for this
  (send-action-legacy! "move-tabs"
                {:window-id (:id window)
                 :index index
                 :tabs (into [] tab-ids)}))

(defn open-window! [active incognito]
  (send-action-legacy! "open-window"
                {:active active :incognito incognito}))

(defn minimize-window! [window]
  (send-action-legacy! "minimize-window"
                {:window-id (:id window)}))

(defn set-window-masked! [window masked]
  (send-action-legacy! "window-masked"
                {:window-id (:id window)
                 :masked masked}))

(defn show-window! [window]
  (send-action-legacy! "show-window"
                {:window-id (:id window)}))


(defn close-window! [window]
  (send-action-legacy! "close-window"
                {:window-id (:id window)}))
  
