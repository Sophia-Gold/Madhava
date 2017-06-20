(ns Madhava.core
  (:require [clojure.pprint :refer [pprint]]
            [clojure.data.int-map :as i]
            [com.rpl.specter :refer :all]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; DIFFERENTIATION & INTEGRATION

(defn diff [poly tape order]
  (letfn [(partial-diff [poly tape idx]
            (let [i (peek idx)
                  key (Long/parseLong (apply str idx))
                  partial (vec
                           (for [expr poly
                                 :let [v (get expr i)]
                                 :when (not (zero? v))] 
                             (-> expr
                                 (update 0 * v)
                                 (update i dec))))] 
              (swap! tape assoc key partial)
              [partial idx]))
          (diff-vars [poly tape idx]
            (map #(partial-diff poly tape (conj idx %))
                 (range 1 (count (first poly)))))
          (diff-loop [poly n]
            (when (<= n order)
              (doseq [x poly]
                (diff-loop
                 (diff-vars (first x) tape (update (second x) 0 inc))
                 (inc n)))))]
  (swap! tape assoc 0 poly)
  (diff-loop [[poly [0]]] 0)))

(defn anti-diff [poly tape order]
  (letfn [(partial-int [poly tape idx]
            (let [i (peek idx)
                  key (Long/parseLong (apply str idx))
                  partial (vec
                           (for [expr poly
                                 :let [v (get expr i)]
                                 :when (not (zero? v))] 
                             (-> expr
                                 (update 0 / (inc v))
                                 (update i inc))))]
              (swap! tape assoc key partial)
              [partial idx]))
          (int-vars [poly tape idx]
            (map #(partial-int poly tape (conj idx %))
                 (range 1 (count (first poly)))))
          (int-loop [poly n]
            (when (<= n order)
              (doseq [x poly]
                (int-loop 
                 (int-vars (first x) tape (update (second x) 0 inc))
                 (inc n)))))]
    (swap! tape assoc 0 poly)
    (int-loop [[poly [0]]] 0)))

(defn pdiff [poly tape order]
  (letfn [(partial-diff [poly tape idx]
            (let [i (peek idx)
                  key (Long/parseLong (apply str idx))
                  partial (vec
                           (for [expr poly
                                 :let [v (get expr i)]
                                 :when (not (zero? v))] 
                             (-> expr
                                 (update 0 * v)
                                 (update i dec))))] 
              (send tape assoc key partial)
              [partial idx]))
          (diff-vars [poly tape idx]
            (pmap #(partial-diff poly tape (conj idx %))
                 (range 1 (count (first poly)))))
          (diff-loop [poly n]
            (when (< n order)
              (doseq [x poly]
                (diff-loop
                 (diff-vars (first x) tape (update (second x) 0 inc))
                 (inc n)))))]
  (send tape assoc 0 poly)
  (diff-loop [[poly [0]]] 0)))

(defn anti-pdiff [poly tape order]
  (letfn [(partial-int [poly tape idx]
            (let [i (peek idx)
                  key (Long/parseLong (apply str idx))
                  partial (vec
                           (for [expr poly
                                 :let [v (get expr i)]
                                 :when (not (zero? v))] 
                             (-> expr
                       (update 0 / (inc v))
                       (update i inc))))]
              (send tape assoc key partial)
              [partial idx]))
          (int-vars [poly tape idx]
            (pmap #(partial-int poly tape (conj idx %))
                 (range 1 (count (first poly)))))
          (int-loop [poly n]
            (when (< n order)
              (doseq [x poly]
                (int-loop 
                 (int-vars (first x) tape (update (second x) 0 inc))
                 (inc n)))))]
    (send tape assoc 0 poly)
    (int-loop [[poly [0]]] 0)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; MACROS

(defmacro diff-once [poly order]
  (let [tape (gensym)]
    `(do
       (def ~tape (atom (i/int-map)))
       (diff ~poly ~tape ~order)
       @~tape)))

(defmacro anti-diff-once [poly order]
  (let [tape (gensym)]
    `(do
       (def ~tape (atom (i/int-map)))
       (anti-diff ~poly ~tape ~order)
       @~tape)))

(defmacro pdiff-once [poly order]
  (let [tape (gensym)]
    `(do
       (def ~tape (agent (i/int-map)))
       (pdiff ~poly ~tape ~order)
       @~tape)))

(defmacro anti-pdiff-once [poly order]
  (let [tape (gensym)]
    `(do
       (def ~tape (agent (i/int-map)))
       (anti-pdiff ~poly ~tape ~order)
       @~tape)))

(defmacro print-map [map]
  `(pprint @~map
           (clojure.java.io/writer
            (str (quote ~map) ".txt"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; HELPER FUNCTIONS

(defn add-dim [poly dim]
  (mapv #(vec (concat (take dim %) [0] (drop dim %))) poly))

(defn remove-terms [term poly]
  (filterv #(not= (next term) (next %)) poly))

(defn denull [poly]
  (filterv #(not= 0 (first %)) poly))

(defn negate [poly]
  (mapv #(update % 0 -) poly))

(defn sort-terms [poly]
;; graded lexicographic order
  (vec
   (sort-by #(- (reduce + (next %)))
            (sort-by (comp - fnext) poly))))

(defn union [intersection & sets]
  (loop [i 0
         test intersection
         difference sets]
    (if (> i (count intersection))
      (into intersection (mapcat identity difference))
      (recur (inc i) (next test) (mapv (partial remove-terms (first test)) difference)))))

(defn intersection [poly1 poly2]
  (vec
   (for [term1 poly1
         term2 poly2
         :when (= (next term1) (next term2))]
     (update term1 0 + (first term2)))))

(defn simplify [poly]
  (loop [idx 0
         test poly
         result []]
    (if (> idx (dec (count poly)))
      result
      (if (= (nfirst test) (nfirst (next test)))
        (recur (+ 2 idx) (nnext test)
               (conj result (vec (cons (+ (ffirst test) (ffirst (next test))) (nfirst test)))))
        (recur (inc idx) (next test) (conj result (first test)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; ARITHMETIC

(defn add [poly1 poly2]
  (sort-terms
   (denull
    (union (intersection poly1 poly2) poly1 poly2))))

(defn sub [poly1 poly2]
  (add poly1 (negate poly2)))

(defn scale [poly scalar]
  (mapv #(update % 0 * scalar) poly))

(defn mul [poly1 poly2]
  (simplify
   (sort-terms
    (for [term1 poly1
          term2 poly2
          :let [coeff (* (first term1) (first term2))]]
      (vec
       (cons coeff
             (for [idx (range 1 (count (first poly1)))]
               (+ (get term1 idx) (get term2 idx)))))))))

(defn compose [f g var]
  (loop [f f
         result []]
    (let [term (first f)]
      (if (nil? term)
        result
        (recur (next f) (if (zero? (nth term var))
                          (add [term] result)
                          (simplify (sort-terms
                                     (vec (concat result
                                                  (nth (iterate (partial mul [(assoc term var 0)]) g)
                                                       (nth term var))))))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; MAP FUNCTIONS

(defn search-map-1 [map val]
  (into (i/int-map)
        (select [ALL (fn [[k v]] (= v val))] map)))

;; filter empty vectors
(defn denull-map [map]
  (setval [MAP-VALS #(= [] %)] NONE map))

(defn transform-map [map f]
  (transform MAP-VALS f map))

(defn add-maps [map1 map2]
  (merge-with add map1 map2))

(defn mul-maps [map1 map2]
  (merge-with mul map1 map2))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; VECTOR OPERATIONS

(defn jacobian [f]
  (vals
   (filter (fn [[k v]]
             (and (> k 9) (< k 100)))
           (diff-once f 1))))

(defn jacobian-indexed [f]
  (filter (fn [[k v]]
            (and (> k 9) (< k 100)))
          (diff-once f 1)))

(defn hessian [f]
  (vals
   (filter (fn [[k v]]
             (and (> k 99) (< k 1000)))
           (diff-once f 2))))

(defn hessian-indexed [f]
  (filter (fn [[k v]]
            (and (> k 99) (< k 1000)))
          (diff-once f 2)))

(defn grad [f]
  (reduce add (jacobian f)))   

(defn div [f vector]
  (reduce add
          (map #(scale %1 %2) (jacobian f) vector)))

(defn curl [f v]
  (let [j (jacobian f)
        j-idx (map-indexed #(vector %1 %2) j)
        v-idx (map-indexed #(vector %1 %2) v)]
    (reduce add
            (concat
             (for [partial j-idx
                   scalar v-idx
                   :when (= (inc (first partial)) (first scalar))]
               (scale (second partial) (second scalar)))
             (vector (scale (last j) (first v)))
             (for [partial j-idx
                   scalar v-idx
                   :when (= (first partial) (inc (first scalar)))]
               (scale (second partial) (- (second scalar))))
             (vector (scale (first j) (- (last v))))))))
  
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TAYLOR SERIES

(defn integrate-series [s]
  (map / s (drop 1 (range))))

(defn negate-series [s]
  (map - s))

(defn sparse-to-dense [s]
  (vec
   (rseq
    (filterv some?
             (mapv #(if (not= %1 0) [%1 %2]) s (range))))))

(defn exp-series []
  (->> (exp-series)
       (integrate-series)
       (lazy-cat [1])))

(declare cos-series)
(defn sin-series []
  (->> (cos-series)
       (integrate-series)
       (lazy-cat [0])))

(defn cos-series []
  (->> (sin-series)
       (negate-series)
       (integrate-series)
       (lazy-cat [1])))

(defn atan-series []
  (integrate-series
   (cycle [1 0 -1 0])))

(declare cosh-series)
(defn sinh-series []
  (->> (cosh-series)
       (integrate-series)
       (lazy-cat [0])))

(defn cosh-series []
  (->> (sinh-series)
       (integrate-series)
       (lazy-cat [1])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main []
  )
