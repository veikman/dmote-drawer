;;; Geometry.

(ns dmote-drawer.models
  (:require [scad-clj.model :as model]
            [scad-tarmi.core :refer [π]]))

(defn base-model
  "Describe the geometry for dmote-drawer."
  [options]
  (model/cube 1 2 4))
