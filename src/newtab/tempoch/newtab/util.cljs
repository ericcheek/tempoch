(ns tempoch.newtab.util)

(defn classes [& items]
  (->>
   items
   (map (fn [x]
          (cond
            (vector? x) (if (first x) (second x))
            :default x)))
   (filter some?)
   (clojure.string/join " ")))

(defn mapall [f items]
  (->> items (map f) doall))

(defn multiple? [l]
  (> (count l) 1))
  
