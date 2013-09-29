(ns nonojure.puzzleview
  (:require
   [dommy.utils :as utils]
   [dommy.core :as dommy])
  (:use-macros
   [dommy.macros :only [node sel sel1]]))

;(def test-data {:left [[] [1 2] [2] [4]]
;                :top [[2] [1 1] [3] [2]]})
;
;(def test-result
;  [:table#table.puzzle-table-non {:width 100 :height 100 :id "puzzle-table"}
;  [:tr [:td]        [:td]        [:td]             [:td#.num 1]      [:td]             [:td]]
;  [:tr [:td]        [:td]        [:td#.num 2]      [:td#.num 1]      [:td#.num 3]      [:td#.num 2]]
;  [:tr [:td]        [:td]        [:td#.cell.c0.r0] [:td#.cell.c1.r0] [:td#.cell.c2.r0] [:td#.cell.c3.r0]]
;  [:tr [:td#.num 1] [:td#.num 2] [:td#.cell.c0.r1] [:td#.cell.c1.r1] [:td#.cell.c2.r1] [:td#.cell.c3.r1]]
;  [:tr [:td]        [:td#.num 2] [:td#.cell.c0.r2] [:td#.cell.c1.r2] [:td#.cell.c2.r2] [:td#.cell.c3.r2]]
;  [:tr [:td]        [:td#.num 4] [:td#.cell.c0.r3] [:td#.cell.c1.r3] [:td#.cell.c2.r3] [:td#.cell.c3.r3]]])

;(def data {:left [[3 1] [3] [2] [1 1] [1 1]]
;           :top [[1 1] [1 3] [2] [1 2] [2]]})

(defn pad-with
  "Given a sequence of numbers adds desired number of nils in the beginning, and to the end"
  ([el nums el-num]
    (let [ els (take el-num (cycle [el]))]
        (into (vec els) nums)))
  ([el nums el-beg el-end]
    (let [ els-beg (take el-beg (cycle [el]))
           els-end (take el-end (cycle [el]))]
        (-> (vec els-beg)
          (into nums)
          (into els-end)))))

(defn create-row [nums offset row-num row-length]
  "Takes a vector of row numbers, offset (assumed to be longer then row numbers vector),
  row number and row length. Returns template for nodes construction"
  (let [ num-tds (map (fn [n] [:td.num.num-not-clicked n]) nums)
         pad-length (- offset (count nums))
         num-tds-left (pad-with [:td.nothing] num-tds pad-length)
         num-tds-right (pad-with [:td.nothing] num-tds 0 pad-length)
         cells (for [c (range row-length)] [(keyword (str "td#.cell.c" c ".r" row-num ".cell-not-clicked"))])]
    (-> [:tr]
      (into num-tds-left)
      (into cells)
      (into num-tds-right))))

(defn create-header [nums offset]
  "Takes a vector of column numbers and offset. Returns template for table header."
  (let [col-num (count nums)
        longest (apply max (map count nums))
        padded  (map #(pad-with nil % (- longest (count %))) nums)]
    (into []
      (for [row (range longest)]
        (let [nums-col (map #(nth % row) padded)
              padded (pad-with nil nums-col offset offset)
              tds (map #(if % [:td.num.num-not-clicked %] [:td.nothing]) padded)]
        (into [:tr] tds))))))

(defn create-bottom [nums offset]
  "Takes a vector of column numbers and offset. Returns template for table bottom."
  (let [col-num (count nums)
        longest (apply max (map count nums))
        padded  (map #(pad-with nil % 0 (- longest (count %))) nums)]
    (into []
      (for [row (range longest)]
        (let [nums-col (map #(nth % row) padded)
              padded (pad-with nil nums-col offset offset)
              tds (map #(if % [:td.num.num-not-clicked %] [:td.nothing]) padded)]
          (into [:tr] tds))))))

(defn create-template [data]
  "Create a template for puzzle based on description"
  (let [ top-nums (get data :top)
         width (count top-nums)
         left-nums (get data :left)
         offset (apply max (map count left-nums))
         header (create-header top-nums offset)
         bottom (create-bottom top-nums offset)
         rows (for [r (range width)]
                (create-row (nth left-nums r)
                            offset
                            r
                            width))]
    (-> [:table#table.puzzle-table-non {:id "puzzle-table"}]
      (into header)
      (into rows)
      (into bottom))))

(defn cell-click [evt node]
  "Change the color when cell is clicked"
  (let [button (.-which evt)
        not-cl (dommy/has-class? node "cell-not-clicked")
        cl     (dommy/has-class? node "cell-clicked")
        r-cl   (dommy/has-class? node "cell-rightclicked")]
    (cond
      (and not-cl (= 1 button)) (do (dommy/remove-class! node "cell-not-clicked")
                                    (dommy/add-class! node "cell-clicked"))
      (and not-cl (= 3 button)) (do (dommy/remove-class! node "cell-not-clicked")
                                    (dommy/add-class! node "cell-rightclicked"))
      (and cl     (= 1 button)) (do (dommy/remove-class! node "cell-clicked")
                                    (dommy/add-class! node "cell-not-clicked"))
      (and cl     (= 3 button)) (do (dommy/remove-class! node "cell-clicked")
                                    (dommy/add-class! node "cell-rightclicked"))
      (and r-cl   (= 1 button)) (do (dommy/remove-class! node "cell-rightclicked")
                                    (dommy/add-class! node "cell-clicked"))
      (and r-cl   (= 3 button)) (do (dommy/remove-class! node "cell-rightclicked")
                                    (dommy/add-class! node "cell-not-clicked"))
      :else false)))


(defn num-click [evt node]
  (let [button (.-which evt)
        not-cl (dommy/has-class? node "num-not-clicked")
        cl     (dommy/has-class? node "num-clicked")]
    (cond
      (and not-cl (or (= 1 button) (= 3 button)))
                                (do (dommy/remove-class! node "num-not-clicked")
                                    (dommy/add-class! node "num-clicked"))
      (and cl (or (= 1 button) (= 3 button)))
                                (do (dommy/remove-class! node "num-clicked")
                                    (dommy/add-class! node "num-not-clicked"))
      :else false)))

(defn add-handlers []
  (let [cells (sel ".cell")
        nums (sel ".num")]
      (doseq [cell cells]
        (set! (.-onmousedown cell) #(cell-click % cell))
        (set! (.-oncontextmenu cell) (fn [evt] false)))
      (doseq [num nums]
        (set! (.-onmousedown num) #(num-click % num))
        (set! (.-oncontextmenu num) (fn [evt] false)))))

(defn show [nono]
  (do
    (dommy/replace! (sel1 :#puzzle-table) (create-template nono))
    (add-handlers)))

(defn ^:export init []
  (when (and js/document
             (aget js/document "getElementById"))
    (dommy/prepend! (sel1 :#puzzle-view) (create-template (nonojure.random/generate-puzzle 10 10)))
    (add-handlers)))