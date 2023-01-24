set terminal png
set output ARG1
set title "Congestion Window vs Time"
set xlabel "Time (s)"
set ylabel "Cwnd"
plot ARG2 using int(ARG3):int(ARG4)  with linespoints title ARG5, ARG6 using int(ARG3):int(ARG7) with linespoints title ARG8