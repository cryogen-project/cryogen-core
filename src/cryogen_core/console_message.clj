(ns cryogen-core.console-message
    (:require [text-decoration.core :refer [blue cyan green red yellow]]))

(defmacro error
  "Print this error `message` to the console."
  [message]
  `(println (red "Error: ") (yellow ~message)))

(defmacro warn
  "Print this warning `message` to the console."
  [message]
  `(println (yellow ~message)))

(defmacro info
  "Print this informational `message` to the console."
  [message]
  `(println (blue ~message)))

(defmacro result
  "Print this result `value` to the console."
  [value]
  `(println "-->" (cyan ~value)))

(defmacro notice
  "Print this notice `message` to the console."
  [message]
  `(println (green ~message)))
