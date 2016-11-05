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
    
(defn activate-tab! [tab-info]
  (send-action! "activate-tab"
                {:tab-id (:id tab-info)
                 :window-id (:windowId tab-info)}))

(defn minimize-window! [window-id]
  (send-action! "minimize-window"
                {:window-id window-id}))

(defn show-window! [window-id]
  (send-action! "show-window"
                {:window-id window-id}))


(defn close-window! [window-id]
  (send-action! "close-window"
                {:window-id window-id}))
  
