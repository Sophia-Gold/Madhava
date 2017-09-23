(ns madhava-test
  (:require [madhava.diff :refer :all]
            [madhava.arithmetic :refer :all]
            [madhava.vectormath :refer :all]
            [madhava.taylorseries :refer :all]
            [clojure.test :refer :all]))

(deftest plus
  (is (= (add [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] [[1 2 1] [4 1 0] [1 0 1]])
         [[1 2 1] [2 1 1] [7 1 0] [6 0 1] [7 0 0]]))
  (is (= (add [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] [[1 2 1] [4 1 0] [1 0 1]] [[10 0 0]])
         [[1 2 1] [2 1 1] [7 1 0] [6 0 1] [17 0 0]]))
  (is (= (add [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] [])
         [[2 1 1] [3 1 0] [5 0 1] [7 0 0]])))

(deftest minus
  (is (= (sub [[2 1 1] [3 1 0] [5 0 1] [7 0 0]])
         [[-2 1 1] [-3 1 0] [-5 0 1] [-7 0 0]]))
  (is (= (sub [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] [[2 1 1] [3 1 0] [5 0 1] [7 0 0]])
         []))
  (is (= (sub [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] [[1 1 1] [3 1 0] [5 0 1] [7 0 0]])
         [[1 1 1]]))
  (is (= (sub [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] [[1 1 1] [3 1 0] [5 0 1]] [[2 0 0]])
         [[1 1 1] [5 0 0]])))

(deftest scaled
  (is (= (scale [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] 2)
         [[4 1 1] [6 1 0] [10 0 1] [14 0 0]]))
  (is (= (scale [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] 1)
         [[2 1 1] [3 1 0] [5 0 1] [7 0 0]])))
  ;; (is (= (scale [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] 0)
  ;;        [])))

(deftest times
  (is (= (mul [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] [[1 2 1] [4 1 0] [1 0 1]])
         [[2 3 2] [3 3 1] [5 2 2] [15 2 1] [2 1 2] [12 2 0] [23 1 1] [5 0 2] [28 1 0] [7 0 1]]))
  (is (= (mul [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] [[1 0 0]])
         [[2 1 1] [3 1 0] [5 0 1] [7 0 0]]))
  ;; (is (= (mul [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] [[0 0 0]])
  ;;        []))
  (is (= (mul [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] [[1 2 1] [4 1 0] [1 0 1]] [[2 0 0]])
         [[4 3 2] [6 3 1] [10 2 2] [30 2 1] [4 1 2] [24 2 0] [46 1 1] [10 0 2] [56 1 0] [14 0 1]])))

(deftest divided
  (is (= (divide [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] [[2 1 1] [3 1 0] [5 0 1] [7 0 0]])
         '([[1 0 0]] [])))
  (is (= (divide [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] [[1 1 1] [7 0 0]])
         '([[5/7 0 1] [1 0 0]] [[3 1 0]]))))

(deftest composed
  (is (= (compose [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] 1)
         [[4 1 2] [12 1 1] [10 0 2] [9 1 0] [34 0 1] [28 0 0]]))
  (is (= (compose [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] [[2 1 1] [3 1 0] [5 0 1] [7 0 0]] 2)
         [[4 2 1] [6 2 0] [20 1 1] [32 1 0] [25 0 1] [42 0 0]])))

(deftest gradient
  (is (= (grad [[8 2 1 2] [1 0 4 0] [2 0 0 3] [5 1 0 0]])
         '([[16 1 1 2] [5 0 0 0]] [[8 2 0 2] [4 0 3 0]] [[16 2 1 1] [6 0 0 2]]))))

(deftest laplace
  (is (= (laplacian [[8 2 1 2] [1 0 4 0] [2 0 0 3] [5 1 0 0]])
         '([[16 0 1 2]] [[12 0 2 0]] [[16 2 1 0] [12 0 0 1]]))))

(deftest divergence
  (is (= (div '([[5 4 3 3]] [[8 2 1 2]] [[1 0 4 0]]))
         '([[20 3 3 3]] [[8 2 0 2]] []))))

(deftest rotation
  (is (= (curl '([[5 4 3 3]] [[8 2 1 2]] [[1 0 4 0]]))
         '([[-16 2 1 1] [4 0 3 0]] [[15 4 3 2]] [[-15 4 2 3] [16 1 1 2]]))))

(deftest series
  (is (= (dense-to-sparse (take 10 (exp-series)))
         [[1/362880 9] [1/40320 8] [1/5040 7] [1/720 6] [1/120 5] [1/24 4] [1/6 3] [1/2 2] [1 1] [1 0]]))
  (is (= (dense-to-sparse (take 10 (sin-series)))
         [[1/362880 9] [-1/5040 7] [1/120 5] [-1/6 3] [1 1]]))
  (is (= (dense-to-sparse (take 10 (cos-series)))
         [[1/40320 8] [-1/720 6] [1/24 4] [-1/2 2] [1 0]]))
  (is (= (dense-to-sparse (take 10 (atan-series)))
         [[1/9 8] [-1/7 6] [1/5 4] [-1/3 2] [1 0]]))
  (is (= (dense-to-sparse (take 10 (sinh-series)))
         [[1/362880 9] [1/5040 7] [1/120 5] [1/6 3] [1 1]]))
  (is (= (dense-to-sparse (take 10 (cosh-series)))
         [[1/40320 8] [1/720 6] [1/24 4] [1/2 2] [1 0]])))

