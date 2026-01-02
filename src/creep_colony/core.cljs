(ns creep-colony.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]))

;; Game constants
(def grid-size 40)
(def initial-energy 100)
(def colony-cost 50)
(def energy-per-creep 0.1)
(def creep-spread-interval 800) ;; ms
(def energy-gain-interval 500) ;; ms
(def win-threshold 0.6) ;; 60% coverage
(def max-colony-reach 10) ;; Maximum distance creep can spread from a colony
(def spread-chance 0.15) ;; Reduced from 0.3 to make spreading slower

;; Cell types
(def EMPTY 0)
(def CREEP 1)
(def COLONY 2)

;; Initialize game state
(defonce game-state
  (r/atom {:grid (vec (repeat grid-size (vec (repeat grid-size EMPTY))))
           :colonies #{}
           :colony-map {} ;; Maps [x y] -> [colony-x colony-y] to track which colony owns each tile
           :energy initial-energy
           :ticks 0
           :won false
           :paused false}))

;; Helper functions
(defn in-bounds? [x y]
  (and (>= x 0) (< x grid-size)
       (>= y 0) (< y grid-size)))

(defn get-cell [grid x y]
  (when (in-bounds? x y)
    (get-in grid [y x])))

(defn set-cell [grid x y value]
  (if (in-bounds? x y)
    (assoc-in grid [y x] value)
    grid))

(defn neighbors [x y]
  (for [dx [-1 0 1]
        dy [-1 0 1]
        :when (not (and (= dx 0) (= dy 0)))]
    [(+ x dx) (+ y dy)]))

(defn has-creep-neighbor? [grid x y]
  (some (fn [[nx ny]]
          (when-let [cell (get-cell grid nx ny)]
            (or (= cell CREEP) (= cell COLONY))))
        (neighbors x y)))

(defn count-cells [grid cell-type]
  (count (filter #(= % cell-type)
                 (flatten grid))))

(defn coverage-percentage [grid]
  (let [total (* grid-size grid-size)
        creep-count (+ (count-cells grid CREEP)
                       (count-cells grid COLONY))]
    (/ creep-count total)))

(defn distance-squared [x1 y1 x2 y2]
  "Calculate squared Euclidean distance (avoids sqrt for performance)"
  (let [dx (- x2 x1)
        dy (- y2 y1)]
    (+ (* dx dx) (* dy dy))))

(defn find-nearest-colony [x y colonies]
  (when (seq colonies)
    (apply min-key
           (fn [[cx cy]] (distance-squared x y cx cy))
           colonies)))

;; Game logic
(defn spread-creep [state]
  (let [{:keys [grid colonies colony-map]} state
        ;; Process each creep/colony tile
        spread-results
        (for [y (range grid-size)
              x (range grid-size)
              :when (#{CREEP COLONY} (get-cell grid x y))
              [nx ny] (neighbors x y)
              :when (and (in-bounds? nx ny)
                         (= (get-cell grid nx ny) EMPTY)
                         (< (rand) spread-chance))]
          ;; Find which colony owns this source tile
          (let [cell-type (get-cell grid x y)
                source-colony (if (= cell-type COLONY)
                                [x y] ;; Colonies own themselves
                                (get colony-map [x y]))] ;; Creep must be in colony-map
            (when source-colony ;; Only spread if we know the owner
              (let [[cx cy] source-colony
                    dist-sq (distance-squared nx ny cx cy)
                    max-reach-sq (* max-colony-reach max-colony-reach)]
                (when (<= dist-sq max-reach-sq)
                  {:pos [nx ny] :colony source-colony})))))
        ;; Filter out nils and apply changes
        valid-spreads (filter some? spread-results)
        new-grid (reduce (fn [g {:keys [pos]}]
                          (let [[x y] pos]
                            (set-cell g x y CREEP)))
                        grid
                        valid-spreads)
        new-colony-map (reduce (fn [m {:keys [pos colony]}]
                                (assoc m pos colony))
                              colony-map
                              valid-spreads)]
    (assoc state
           :grid new-grid
           :colony-map new-colony-map)))

(defn gain-energy [state]
  (let [creep-count (+ (count-cells (:grid state) CREEP)
                       (count-cells (:grid state) COLONY))
        energy-gain (* creep-count energy-per-creep)]
    (update state :energy + energy-gain)))

(defn place-colony [state x y]
  (let [{:keys [grid energy colonies colony-map]} state
        cell (get-cell grid x y)]
    (cond
      (< energy colony-cost)
      state ;; Not enough energy

      (not (#{CREEP COLONY} cell))
      state ;; Can only place on creep or replace colony

      :else
      (-> state
          (update :grid set-cell x y COLONY)
          (update :colonies conj [x y])
          (update :colony-map assoc [x y] [x y]) ;; Colony owns itself
          (update :energy - colony-cost)))))

(defn check-victory [state]
  (let [coverage (coverage-percentage (:grid state))]
    (if (>= coverage win-threshold)
      (assoc state :won true)
      state)))

(defn init-game []
  (let [center (quot grid-size 2)
        initial-grid (-> (vec (repeat grid-size (vec (repeat grid-size EMPTY))))
                         (set-cell center center COLONY))]
    (reset! game-state
            {:grid initial-grid
             :colonies #{[center center]}
             :colony-map {[center center] [center center]} ;; Initial colony owns itself
             :energy initial-energy
             :ticks 0
             :won false
             :paused false})))

;; Game loop
(defn game-tick []
  (when-not (:paused @game-state)
    (swap! game-state
           (fn [state]
             (let [new-state (-> state
                                 (update :ticks inc))]
               (cond-> new-state
                 (zero? (mod (:ticks new-state) 3)) ;; Slower spread: every 3 ticks instead of 2
                 (spread-creep)

                 (zero? (mod (:ticks new-state) 1))
                 (gain-energy)

                 true
                 (check-victory)))))))

;; UI Components
(defn grid-cell [x y cell]
  (let [can-afford? (>= (:energy @game-state) colony-cost)
        is-creep? (= cell CREEP)
        can-place? (and can-afford? is-creep?)]
    [:div.grid-cell
     {:class (case cell
               0 "empty"
               1 "creep"
               2 "colony"
               "empty")
      :title (str "(" x "," y ")")
      :style {:cursor (if can-place? "pointer" "default")}
      :on-click (fn []
                  (when (and can-place? (not (:won @game-state)))
                    (swap! game-state place-colony x y)))}]))

(defn game-grid []
  (let [{:keys [grid]} @game-state]
    [:div {:style {:text-align "center"}}
     [:div.game-grid
      {:style {:grid-template-columns (str "repeat(" grid-size ", 14px)")}}
      (for [y (range grid-size)
            x (range grid-size)]
        ^{:key (str x "-" y)}
        [grid-cell x y (get-cell grid x y)])]]))

(defn game-stats []
  (let [{:keys [energy grid]} @game-state
        coverage (* 100 (coverage-percentage grid))
        creep-tiles (+ (count-cells grid CREEP) (count-cells grid COLONY))]
    [:div.game-info
     [:div.stat
      [:div.stat-value (Math/floor energy)]
      [:div.stat-label "Energy"]]
     [:div.stat
      [:div.stat-value creep-tiles]
      [:div.stat-label "Creep Tiles"]]
     [:div.stat
      [:div.stat-value (.toFixed coverage 1) "%"]
      [:div.stat-label "Map Coverage"]]
     [:div.stat
      [:div.stat-value colony-cost]
      [:div.stat-label "Colony Cost"]]]))

(defn victory-screen []
  (when (:won @game-state)
    [:div.victory
     [:h2 "🎉 VICTORY! 🎉"]
     [:p {:style {:font-size "1.3em"}}
      (str "You've conquered " (.toFixed (* 100 (coverage-percentage (:grid @game-state))) 1) "% of the map!")]
     [:p "The creep spreads victorious across the lands."]
     [:button
      {:on-click init-game}
      "Play Again"]]))

(defn controls []
  [:div {:style {:text-align "center" :margin "20px 0"}}
   [:button
    {:on-click #(swap! game-state update :paused not)}
    (if (:paused @game-state) "Resume" "Pause")]
   [:button
    {:on-click init-game}
    "New Game"]])

(defn instructions []
  [:div.instructions
   [:h3 "How to Play"]
   [:ul
    [:li "You start with a single Creep Colony (glowing purple circle) in the center"]
    [:li "Creep (purple tiles) spreads automatically from colonies"]
    [:li "Each colony has a maximum reach of " max-colony-reach " tiles"]
    [:li "Creep generates Energy over time"]
    [:li "Click on any creep tile to place a new colony (costs " colony-cost " energy)"]
    [:li "Place colonies strategically to extend your reach!"]
    [:li "Goal: Cover " (* 100 win-threshold) "% of the map to win!"]]])

(defn app []
  [:div#app
   [:h1.game-title "⚡ CREEP COLONY ⚡"]
   [game-stats]
   [controls]
   [game-grid]
   [instructions]
   [victory-screen]])

;; Initialize
(defn ^:export init! []
  (init-game)
  (rdom/render [app] (.getElementById js/document "app"))
  ;; Start game loop
  (js/setInterval game-tick (/ creep-spread-interval 2)))
