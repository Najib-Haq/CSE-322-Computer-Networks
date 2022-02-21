set terminal png
set output ARG1
set title ARG2
set xlabel ARG3
set ylabel ARG4
plot ARG5 using int(ARG6):int(ARG7)  with linespoints title ARG4