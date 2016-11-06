(ns tempoch.newtab.state
  (:require
   [reagent.core :as reagent]
   [chromex.logging :refer-macros [log info warn error group group-end]]))

(defonce local-ctx
  (reagent/atom {:drag-state nil}))

(defonce app-ctx
  (reagent/atom {}))
