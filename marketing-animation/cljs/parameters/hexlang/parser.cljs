(ns parameters.hexlang.parser
  (:require [instaparse.core :as insta]
            [clojure.string :as cstr]))

(def ^:private hexlang-xform
  {:param-literal cstr/trim
   :value-literal cstr/trim})

(defn- parse-pv-declarations*
  [parser text]
  (into []
        (comp (filter #(not-empty (cstr/trim %)))
              (map parser))
        (cstr/split-lines text)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Legacy grammar

(def legacy-hexlang-grammar
  "
param-literal = #'((\\\\:)|[^:])+';

value-literal = #'((\\\\,)|[^,])+';

<pv-decl> = param-literal colon value-list <opt-whitespace>;
<value-list> =
    value-literal
  | value-literal <comma> value-list;

<comma>            =  <','>;
<colon>            =  <':'>;
<opt-whitespace>   =  <#'\\s*'>;
  ")

(def legacy-hexlang-parser
  (comp #(insta/transform hexlang-xform %)
        (insta/parser legacy-hexlang-grammar :start :pv-decl)))

(def parse-legacy-pv-declarations
  (partial parse-pv-declarations* legacy-hexlang-parser))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; New grammar

(def hexlang-grammar
  "
param-literal = #'((\\\\\\[)|[^[])+';

value-literal = #'((\\\\,)|(\\\\])|[^,\\]])+';

<pv-decl> = param-literal value-set <opt-whitespace>;
<value-set> = open-sq-bracket value-list close-sq-bracket;
<value-list> =
    value-literal
  | value-literal <comma> value-list;

<open-sq-bracket>  =  <'['>;
<close-sq-bracket> =  <']'>;
<comma>            =  <','>;
<colon>            =  <':'>;
<opt-whitespace>   =  <#'\\s*'>;
  ")

(def hexlang-parser
  (comp #(insta/transform hexlang-xform %)
        (insta/parser hexlang-grammar :start :pv-decl)))

(def parse-pv-declarations
  (partial parse-pv-declarations* hexlang-parser))