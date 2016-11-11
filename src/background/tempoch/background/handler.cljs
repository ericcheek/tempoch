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
       (apply
        api-fn
        (->> args (map clj->js) into-array)))]))

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

(defn set-persistent-state! [edn-value]
  (swap! state/ctx
         assoc :persistent
         (cljs.reader/read-string edn-value)))

(defn set-transient-state! [edn-value]
  (swap! state/ctx
         assoc :transient
         (cljs.reader/read-string edn-value)))

(def handlers
  (->
   {:td/set-transient set-transient-state!}
   (merge chrome-handlers)))

(defn handle-client-requests! [message]
  (let [action (aget message "action")
        params (aget message "params")]
    (cond
      (= action "batch-ops")
      (go
        (doseq [[action-key & args] (cljs.reader/read-string params)]
          (apply (get handlers action-key) args)))

      :default
      (error "No handler defined for message " message)
      )))
