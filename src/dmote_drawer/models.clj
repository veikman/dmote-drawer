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
(def dfm-margins [0.5 0.25 0.6])
(def drawer-size (mapv - port-size dfm-margins))
(def handle-width-outer 8)
(def handle-width-inner 3)

(def magnet-inset 4)  ; From the back of the port.
(def magnet-margin 0.25)
(def r-rear 18)
(def r-lesser 3)
(def r-fillet 1.5)
(def outer-cigar-end-size [40 24 22])
(def inner-cigar-end-size [20 8 12])
(def outer-cigar-end-position [8 -7 (/ (nth drawer-size 2) 2)])
(def inner-cigar-end-position [6 3 (/ (nth drawer-size 2) 2)])

(def screw-xy (+ magnet-inset magnet-margin))
(def screw-position [(- (/ (first drawer-size) 2) screw-xy)
                     (- (second drawer-size) screw-xy)])

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
      (model/offset (- r-fillet)
        (model/hull
          (model/translate [(+ (/ x -2) r-rear) (- y r-rear)] (model/circle r-rear))
          (model/translate [(- (/ x 2) r-rear) (- y r-rear)] (model/circle r-rear))
          (model/translate [(+ (/ x -2) r-lesser) r-lesser] (model/circle r-lesser))
          (model/translate [(- (/ x 2) r-lesser) r-lesser] (model/circle r-lesser)))))))

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
  [base offset]
  (model/resize (map (partial + offset) base)
    (model/sphere (/ (first base) 2))))

(defn- cigar-model
  "Negative or positive space for an alcove for the drawer’s handle."
  [position base-size offset]
  (model/hull
    (model/translate position (cigar-end base-size offset))
    (model/translate (update position 0 -) (cigar-end base-size offset))))

(defn- cigar-pair
  [offset]
  (model/hull
    (cigar-model outer-cigar-end-position outer-cigar-end-size offset)
    (cigar-model inner-cigar-end-position inner-cigar-end-size offset)))

(def cigar-core (cigar-pair 0))
(def cigar-shell (cigar-pair wall-thickness))

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
      (model/translate [0 0 (+ floor-thickness r-fillet)]
        (model/minkowski
          (model/extrude-linear {:height (nth drawer-size 2), :center false}
                                inner-contour)
          (model/sphere r-fillet)))
      (model/hull
        cigar-shell
        (model/translate [0 0 (- big)] cigar-shell)))
    (model/translate screw-position magnet-target)
    (model/translate (update screw-position 0 -) magnet-target)
    (model/difference
      cigar-core
      (model/hull
        (model/cube handle-width-outer wall-thickness big)
        (model/translate [0 8 0] (model/cube handle-width-inner 1 big))))))
