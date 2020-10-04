;;; Geometry.

(ns dmote-drawer.models
  (:require [scad-clj.model :as model]
            [scad-tarmi.core :refer [π]]
            [scad-tarmi.dfm :refer [error-fn]]
            [scad-klupe.iso :refer [bolt]]))

(def port-size [162 92 25])
(def wall-thickness 2)
(def dfm-margins [0.5 0.25 0.15])
(def magnet-inset 4)  ; From the back of the port.
(def r-rear 15)
(def r-lesser 3)
(def drawer-size (mapv - port-size dfm-margins))
(def screw-position [(- (/ (first drawer-size) 2) magnet-inset)
                     (- (second drawer-size) magnet-inset)])

(let [[x y _] drawer-size
      depth (- y r-lesser)]
  (def outer-contour
    (model/union
      (model/translate [0 depth]
        (model/hull
          (model/translate [(+ (/ x -2) r-lesser) 0] (model/circle r-lesser))
          (model/translate [(- (/ x 2) r-lesser) 0] (model/circle r-lesser))))
      (model/translate [0 (/ depth 2)] (model/square x depth)))))

(let [[x y _] (mapv - drawer-size (repeat 3 (* 2 wall-thickness)))]
  (def inner-contour
    (model/translate [0 wall-thickness]
      (model/hull
        (model/translate [(+ (/ x -2) r-rear) (- y r-rear)] (model/circle r-rear))
        (model/translate [(- (/ x 2) r-rear) (- y r-rear)] (model/circle r-rear))
        (model/translate [(+ (/ x -2) r-lesser) r-lesser] (model/circle r-lesser))
        (model/translate [(- (/ x 2) r-lesser) r-lesser] (model/circle r-lesser))))))

(def magnet-target
  (model/translate [0 0 0.5]
    (model/rotate [π 0 0]
      (bolt {:m-diameter 3
             :head-type :countersunk
             :total-length (nth port-size 2)
             :channel-length 2
             :compensator (error-fn 0.5)}))))

(defn base-model
  "Describe the geometry for one drawer."
  [options]
  (model/difference
    (model/extrude-linear {:height (nth drawer-size 2), :center false}
                          outer-contour)
    (model/translate [0 0 wall-thickness]
      (model/extrude-linear {:height (nth drawer-size 2), :center false}
                            inner-contour))
    (model/translate screw-position magnet-target)
    (model/translate (update screw-position 0 -) magnet-target)))
