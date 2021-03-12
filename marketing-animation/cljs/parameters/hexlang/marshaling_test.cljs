(ns parameters.hexlang.marshaling-test
  (:require [parameters.hexlang.marshaling :as sut]
            [clojure.test :refer-macros [deftest is]]
            ))

(def sample-legacy-text
  "A: A1, A2, A3
B: B1, B2
C: C1, C2, C3
Fruit: Apple, Pear, Apple Sauce
Delicious Dessert: Chocolate Pudding, Banana Pudding
Caramel: That'd be crazy!, Yes\\, please, [Squares]
Crazy\\:Param: Z1, Z2
Crazier[Param]: Y1, Y2
")

(def sample-text
  "A[A1, A2, A3]
B[B1, B2]
C[C1, C2, C3]
Fruit[Apple, Pear, Apple Sauce]
Delicious Dessert[Chocolate Pudding, Banana Pudding]
Caramel[That'd be crazy!, Yes\\, please, [Squares\\]]
Crazy:Param[Z1, Z2]
Crazier\\[Param][Y1, Y2]
")

(deftest transpilation-tests
  (is (= (sut/unmarshal-pv-declarations sample-legacy-text)
         sample-text))
  (is (= (sut/marshal-pv-declarations sample-text)
         sample-legacy-text))
  (is (= sample-legacy-text
         (-> sample-legacy-text
             (sut/unmarshal-pv-declarations)
             (sut/marshal-pv-declarations))))
  (is (= sample-text
         (-> sample-text
             (sut/marshal-pv-declarations)
             (sut/unmarshal-pv-declarations)))))
