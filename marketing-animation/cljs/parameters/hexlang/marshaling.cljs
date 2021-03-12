(ns parameters.hexlang.marshaling
  (:require [parameters.hexlang.parser :as parser]
            [clojure.string :as cstr]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Marshaling
;;  - transpiles today's format to the legacy format

(defn marshal-pv-declarations
  [text]
  (letfn [(escape-param [param]
            (-> param
                (cstr/replace #"\\\[" "[")
                (cstr/replace #":" "\\:")))
          (escape-value [value]
            (cstr/replace value #"\\]" "]"))]
    (cstr/join
     (reduce (fn [decls [param & values]]
               (let [clean-values (map escape-value values)]
                 (into decls [(escape-param param)
                              ": " (cstr/join ", " clean-values) "\n"])))
             []
             (parser/parse-pv-declarations text)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Unmarshaling
;;  - transpiles legacy format to today's format

(defn unmarshal-pv-declarations
  [text]
  (letfn [(escape-param [param]
            (-> param
                (cstr/replace #"\[" "\\[")
                (cstr/replace #"\\:" ":")))
          (escape-value [value]
            (cstr/replace value #"]" "\\]"))]
    (cstr/join
     (reduce (fn [decls [param & values]]
               (let [clean-values (map escape-value values)]
                 (into decls [(escape-param param)
                              "[" (cstr/join ", " clean-values) "]\n"])))
             []
             (parser/parse-legacy-pv-declarations text)))))