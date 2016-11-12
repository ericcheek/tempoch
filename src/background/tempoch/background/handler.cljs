(ns tempoch.background.handler
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [goog.string :as gstring]
            [goog.string.format]
            [cljs.core.async :refer [<! chan]]
            [cemerick.url :refer (url url-encode)]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.ext.tabs :as tabs]
            [chromex.ext.windows :as windows]
            [chromex.ext.runtime :as runtime]
            [tempoch.background.state :as state]))

(defn- chrome-api-handler-entry [kw]
  (let [api-fn (-> js/chrome
                   (aget (namespace kw))
                   (aget (name kw)))]
    [kw
     (fn [& args]
       (go
         (apply
          api-fn
          (->> args (map clj->js) into-array))))]))

(defn open-window-with-tabs [{[tab-id & tab-ids] :tabIds :as options}]
  (go
    (let
        [options' (->
                   options
                   (assoc :tabId tab-id)
                   (dissoc :tabIds)
                   clj->js)
         [window] (<! (windows/create options'))
         window-id (aget window "id")]
      (when (pos? (count tab-ids))
        (tabs/move
         (clj->js tab-ids)
         (clj->js
          {:windowId window-id
           :index -1}))))))

;; caution: these will be directly invokable by incoming messages.
;; requires some reconsideration with content script messaging or expanded api coverage
(def chrome-handlers
  (->>
   [:windows/create
    :windows/update
    :windows/remove
    :tabs/create
    :tabs/remove
    :tabs/update
    :tabs/move
    :tabs/reload
    :tabs/discard]
   (map chrome-api-handler-entry)
   (into {})))

(def handlers
  (->
   {:td/open-window-with-tabs open-window-with-tabs
    :td/set-transient state/set-transient!
    :td/set-persistent state/set-persistent!}
   (merge chrome-handlers)))

(defn handle-client-requests! [message]
  (let [action (aget message "action")
        params (aget message "params")]
    (cond
      (= action "batch-ops")
      (doseq [[action-key & args] (cljs.reader/read-string params)]
        (apply (get handlers action-key) args))

      :default
      (error "No handler defined for message " message)
      )))
