(ns parameters.hexlang.parser-test
  (:require [clojure.test :refer-macros [deftest is]]
            [parameters.hexlang.parser :as sut]
            ))

(def sample-legacy-text
  "
A: A1, A2, A3
B: B1, B2
C: C1, C2, C3
Fruit: Apple, Pear, Apple Sauce
Delicious Dessert: Chocolate Pudding, Banana Pudding
Caramel: That'd be crazy!, Yes\\, please
Crazy\\:Param: Z1, Z2
")

(deftest legacy-grammar-parsing-test
  (is (= (sut/parse-legacy-pv-declarations sample-legacy-text)
         [["A" "A1" "A2" "A3"]
          ["B" "B1" "B2"]
          ["C" "C1" "C2" "C3"]
          ["Fruit" "Apple" "Pear" "Apple Sauce"]
          ["Delicious Dessert" "Chocolate Pudding" "Banana Pudding"]
          ["Caramel" "That'd be crazy!" "Yes\\, please"]
          ["Crazy\\:Param" "Z1" "Z2"]]
         )))

(def sample-text
  "
A[A1, A2, A3]
B[B1, B2]
C[C1, C2, C3]
Fruit[Apple, Pear, Apple Sauce]
Delicious Dessert[Chocolate Pudding, Banana Pudding]
Caramel[That'd be crazy!, Yes\\, please]
Crazy\\[Param][Z1, Z2]
")

(deftest grammar-parsing-test
  (is (= (sut/parse-pv-declarations sample-text)
         [["A" "A1" "A2" "A3"]
          ["B" "B1" "B2"]
          ["C" "C1" "C2" "C3"]
          ["Fruit" "Apple" "Pear" "Apple Sauce"]
          ["Delicious Dessert" "Chocolate Pudding" "Banana Pudding"]
          ["Caramel" "That'd be crazy!" "Yes\\, please"]
          ["Crazy\\[Param]" "Z1" "Z2"]]
         )))