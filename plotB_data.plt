set terminal png
set output ARG1
set title ARG2
set xlabel ARG3
set ylabel "Throughput"
plot ARG4 using int(ARG5):int(ARG6)  with linespoints title "newreno", ARG4 using int(ARG5):int(ARG7) with linespoints title ARG8