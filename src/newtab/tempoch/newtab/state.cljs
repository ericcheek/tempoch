(ns tempoch.newtab.state
  (:require
   [reagent.core :as reagent]
   [chromex.logging :refer-macros [log info warn error group group-end]]))

(defonce local-ctx
  (reagent/atom {:drag-state nil
                 :tab-selection #{}}))

(defonce app-ctx
  (reagent/atom {}))

(defn get-drag-state []
  (:drag-state @local-ctx))

(defn is-drag-active? []
  (some? (get-drag-state)))

(defn set-drag-selection! [selection]
  (swap! local-ctx
         assoc :drag-state
         {:selection selection}))

(defn get-drag-selection []
  (get-in [:drag-state :selection] @local-ctx))

(defn set-drop-fn! [drop-fn]
  (swap! local-ctx assoc-in [:drag-state :drop-fn] drop-fn))

(defn get-drop-fn []
  (get-in [:drag-state :drop-fn] @local-ctx))

(defn clear-drag-state! []
  (swap! local-ctx
         assoc :drag-state
         nil))


(defn get-tab-selection []
  (:tab-selection @local-ctx))

(defn add-to-selection! [& tabs]
  (swap! local-ctx
         update :tab-selection
         into (map :id tabs)))

(defn remove-from-selection! [& tabs]
  (swap! local-ctx
         update :tab-selection
         clojure.set/difference
         (into #{} (map :id tabs))))

(defn is-tab-selected? [tab]
  (contains?
   (get-tab-selection)
   (:id tab)))
  

(defn clear-selection! []
  (swap! local-ctx assoc :tab-selection #{}))
