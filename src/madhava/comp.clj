(ns madhava.comp
  (:require [madhava.arithmetic :refer :all]
            [madhava.vectormath :refer :all]
            [madhava.util :refer :all]
            [clojure.data.int-map :as i]))

(defn compose [f g idx]
  (let [idx (dec idx)  ;; 1st var (x) == 0th index
        dims (dims (first (last f)))]
    (loop [f f
           result (i/int-map)]
      (if (empty? f)
        result
        (let [term (first f)
              vars (first term)
              coeff (second term)
              v (int-nth vars idx dims)]
          (if (zero? v)
            (recur (dissoc f vars)
                   (add (i/int-map vars coeff) result))
            (recur (dissoc f vars)
                   (add (apply mul
                               (i/int-map (- vars
                                             (* v (long (Math/pow 10 (- (dec dims) idx)))))  ;; elide var at idx
                                          coeff)
                               (repeat v g))  ;; raise g to exponent
                        result))))))))

(defn chain
  ;; transducer form of compose
  ([f]  ;; completion
   (fn
     ([] f)
     ([g] (chain f g))
     ([g & more] (chain f g more))))
  ([f g]
   (let [dims (dims (first (last f)))
         grad-f (grad f)
         grad-g (grad g)
         f-g (map #(compose f g (inc %)) (range dims))]
     (map add
          (map-indexed #(mul (nth grad-f %1) %2) f-g)
          (map-indexed #(mul (nth grad-g %1) %2) f-g))))
  ([f g & more]
   (reduce chain (chain f g) more)))

(defn rchain
  ;; transducer form of compose
  ([f]  ;; completion
   (fn
     ([] f)
     ([f g] (rchain f g))
     ([f g & more] (apply rchain f g more))))
  ([f g]
   (let [dims (dims (first (last g)))
         grad-f (grad f)
         grad-g (grad g)
         g-f (map #(compose g f (inc %)) (range dims))]  ;; transpose matroid
     (map add                                                  
          (map-indexed #(mul (nth grad-f %1) %2) g-f)
          (map-indexed #(mul (nth grad-g %1) %2) g-f))))
  ([f g & more]
   (reduce rchain (rseq (conj more (rchain f g))))))  ;; reverse mode AD is just reversing a vector!
