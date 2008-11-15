(clojure.core/ns com.example.anglediff)

; See property fail, but tests succeed.
(defn anglediff
  "Compute the angular difference between a and b."
  [a b]
  (rem (Math/abs (- a b)) 180))

(comment
; See property and tests succeed.
(defn anglediff
  "Compute the angular difference between a and b."
  [a b]
  (let [diff (rem (Math/abs (- a b)) 360)]
    (if (< 180 diff)
      (- 360 diff)
      diff)))
  )
