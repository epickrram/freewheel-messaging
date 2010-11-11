set title "Ops per second for ByteOutputBuffer\nvs nio.ByteBuffer"
#set xtics nomirror rotate by 45
set format y "%20.0f"
set key noinvert reverse outside
#set key noinvert box
set style data linespoints
set linestyle 1
set pointsize 5
set terminal gif small size 900,300
# size 600,300
set output "/tmp/byte_buffer_int.gif"
plot "/tmp/ByteArrayOutputBufferPerfTest-perf-results.csv" using 2:xtic(1) ti columnheader(1) with linespoints, '' u 3 ti columnheader(2) with linespoints, '' u 4 ti columnheader(3) with linespoints, '' u 5 ti columnheader(4) with linespoints, '' u 6 ti columnheader(5) with linespoints, '' u 7 ti columnheader(6) with linespoints
