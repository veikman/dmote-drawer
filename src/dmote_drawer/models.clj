;;; Geometry.

(ns dmote-drawer.models
  (:require [scad-clj.model :as model]
            [scad-tarmi.core :refer [π]]
            [scad-tarmi.dfm :refer [error-fn]]
            [scad-klupe.iso :refer [bolt]]))

(def big 100)

(def port-size [162 92 25])
(def wall-thickness 2)
(def floor-thickness 1)
(def dfm-margins [0.5 0.25 0.15])
(def drawer-size (mapv - port-size dfm-margins))

(def magnet-inset 4)  ; From the back of the port.
(def r-rear 18)
(def r-lesser 3)
(def cigar-end-size [38 22 22])
(def cigar-offset 4)
(def cigar-end-position [10 -6 (/ (nth drawer-size 2) 2)])

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

(let [leeway 3]
  (def magnet-target
    "A hole for a screw for keeping a drawer in place, by interacting, at
    adjustable strength, with a magnet in the shelf underneath the drawer."
    (->> (bolt {:m-diameter 3
                :head-type :countersunk
                :total-length (- (nth port-size 2) leeway)
                :channel-length leeway
                :compensator (error-fn 0.5)})
      (model/rotate [π 0 0])
      (model/translate [0 0 leeway]))))

(defn- cigar-end
  [coefficient]
  (model/resize (map (partial * coefficient) cigar-end-size)
    (model/sphere (/ (first cigar-end-size) 2))))

(defn- cigar-model
  "Negative or positive space for an alcove for the drawer’s handle."
  [coefficient]
  (model/hull
    (model/translate cigar-end-position (cigar-end coefficient))
    (model/translate (update cigar-end-position 0 -) (cigar-end coefficient))))

(def cigar-shell (cigar-model 1.2))
(def drawer-shell
  (model/extrude-linear {:height (nth drawer-size 2), :center false}
                        outer-contour))

(defn base-model
  "Describe the geometry for one drawer."
  [options]
  (model/difference
    drawer-shell
    (model/translate [0 (- big) 0]
      (model/cube (* 2 big) (* 2 big) big))
    (model/difference
      (model/translate [0 0 floor-thickness]
        (model/extrude-linear {:height (nth drawer-size 2), :center false}
                              inner-contour))
      (model/hull
        cigar-shell
        (model/translate [0 0 (- big)] cigar-shell)))
    (model/translate screw-position magnet-target)
    (model/translate (update screw-position 0 -) magnet-target)
    (model/difference
      (cigar-model 1)
      (model/hull
        (model/cube 4 wall-thickness big)
        (model/translate [0 6 0] (model/cube 1 1 big))))))
