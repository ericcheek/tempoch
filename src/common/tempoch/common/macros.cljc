(ns tempoch.common.macros)


(defmacro evfn [params & body]
  (let [{stop :stop
         prevent :prevent} (meta params)]
    `(fn [& [e# :as args#]]
       (let [~params args#]
         (when ~stop (.stopPropagation e#))
         (when ~prevent (.preventDefault e#))
         ~@body
         nil))))

(defmacro time-operation [label & body]
  `(let
       [start-time# (js/window.performance.now)
        result# (do ~@body)]
     (js/console.log ~label (- (js/window.performance.now) start-time#))
     result#))
