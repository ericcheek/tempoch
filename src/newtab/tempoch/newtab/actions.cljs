(ns tempoch.newtab.actions
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [cemerick.url :as url-helper]
            [chromex.protocols :refer [post-message!]]
            [tempoch.newtab.state :as state]))

(defn format-query [q]
  (or
   (and (or
         (.startsWith q "http://")
         (.startsWith q "https://")) q)
   (and (some-> q url-helper/url :host .-length (> 0)) (str "http://" q))
   (str"https://duckduckgo.com/?q=" (url-helper/url-encode q))))

(defn send-action! [action params]
  (post-message!
   (:bg-port @state/app-ctx)
   (clj->js
    {:action action
     :params params})))

(defn send-ops! [& action-specs]
  (send-action!
   "batch-ops"
   (pr-str (into [] action-specs))))


(defn activate-tab! [tab]
  (send-ops!
   [:tabs/update (:id tab) {:active true}]
   [:windows/update (:windowId tab) {:focused true}]))

(defn navigate-tab! [tab query]
  (send-ops!
   [:tabs/update (:id tab) {:url (format-query query)}]))

(defn close-tab! [tab]
  (send-ops!
   [:tabs/remove (:id tab)]))

(defn open-tab! [window query switch-to]
  (send-ops!
   [:tabs/create {:windowId (:id window)
                  :url (format-query query)
                  :active switch-to}]))

(defn move-tabs! [window index tab-ids]
  (state/clear-selection!) ;; feels like the wrong place for this
  (send-ops!
   [:tabs/move
    (into [] tab-ids)
    {:windowId (:id window)
     :index index}]))

(defn open-window! [active incognito]
  (send-ops!
   [:windows/create {:incognito incognito
                    :state (if active "normal" "minimized")}]))

(defn minimize-window! [window]
  (send-ops!
   [:windows/update (:id window) {:state "minimized"}]))

(defn set-window-masked! [window masked]
  (send-ops!
   [:td/set-persistent
    (->
     (state/get-persistent)
     (assoc-in [:windows (:id window) :masked] masked)
     pr-str)]))

(defn show-window! [window]
  (send-ops!
   [:windows/update (:id window) {:state "normal"}]))

(defn close-window! [window]
  (send-ops!
   [:windows/remove (:id window)]))
