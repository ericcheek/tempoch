(ns tempoch.newtab.actions
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols :refer [post-message!]]
            [tempoch.newtab.state :as state]))

(defn send-action! [action params]
  (post-message!
   (:bg-port @state/app-ctx)
   (clj->js
    {:action action
     :params params})))
    
(defn activate-tab! [tab]
  (send-action! "activate-tab"
                {:tab-id (:id tab)
                 :window-id (:windowId tab)}))

(defn navigate-tab! [tab query]
  (send-action! "navigate-tab"
                {:tab-id (:id tab)
                 :query query}))

(defn close-tab! [tab]
  (send-action! "close-tab"
                {:tab-id (:id tab)}))

(defn open-tab! [window query switch-to]
  (send-action! "open-tab"
                {:window-id (:id window)
                 :query query
                 :active switch-to}))

(defn move-tabs! [window index tab-or-tab-ids]
  (send-action! "move-tabs"
                {:window-id (:id window)
                 :index index
                 :tabs tab-or-tab-ids}))

(defn open-window! [active incognito]
  (send-action! "open-window" {:active active :incognito incognito}))

(defn minimize-window! [window]
  (send-action! "minimize-window"
                {:window-id (:id window)}))

(defn set-window-masked! [window masked]
  (send-action! "window-masked"
                {:window-id (:id window)
                 :masked masked}))

(defn show-window! [window]
  (send-action! "show-window"
                {:window-id (:id window)}))


(defn close-window! [window]
  (send-action! "close-window"
                {:window-id (:id window)}))
  
