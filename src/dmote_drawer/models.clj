;;; Geometry.

(ns dmote-drawer.models
  (:require [scad-clj.model :as model]
            [scad-tarmi.core :refer [π]]))

(def port-size [162 92 25])
(def wall-thickness 2)
(def dfm-margins [0.5 0.25 0.15])

(defn base-model
  "Describe the geometry for one drawer."
  [options]
  (model/difference
    (apply model/cube (mapv - port-size dfm-margins))
    (model/translate [0 0 wall-thickness]
      (apply model/cube (mapv - port-size
                                (repeat 3 (* 2 wall-thickness))
                                dfm-margins)))))
