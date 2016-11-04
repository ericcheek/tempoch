(ns tempoch.newtab.actions
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols :refer [post-message!]]))

(defn activate-tab [bg-port tab-info]
  (post-message!
   bg-port
   (clj->js
    {:action "activate-tab"
     :data {:tab-id (:id tab-info)
            :window-id (:windowId tab-info)}})))
