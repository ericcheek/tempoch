(ns tempoch.background.handler
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [tempoch.background.macros :refer [fn-lookup-table]])
  (:require [goog.string :as gstring]
            [goog.string.format]
            [cljs.core.async :refer [<! chan]]
            [cemerick.url :refer (url url-encode)]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.ext.tabs :as tabs]
            [chromex.ext.windows :as windows]
            [chromex.ext.runtime :as runtime]
            [tempoch.background.state :as state]))

;; caution: these will be directly invokable by incoming messages. requires some reconsideration with content script messaging or expanded api coverage
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
   (map (fn [kw]
          [kw (->
               js/chrome
               (aget (namespace kw))
               (aget (name kw)))]))
   (into {})))
   
(def handlers
  {
  
   :window-masked
   (fn [{:keys [window-id masked]}]
     (swap! state/ctx
            assoc-in [:transient :windows window-id :masked]
            masked))
   })
                    

(defn handle-client-requests! [message]
  (log message)
  (let [action (aget message "action")
        params (aget message "params")
        legacy-handler (get handlers (keyword action))]
    (cond
      (some? legacy-handler) (legacy-handler
                              (js->clj params :keywordize-keys true))
      
      (= action "passthrough")
      (go        
        (doseq [[action-key & args :as command] (cljs.reader/read-string params)]
          (apply
           (get chrome-handlers action-key)
           (->> args (map clj->js) into-array))))
      
      :default
      (error "No handler defined for message " message)
      )))    
  
