#!/bin/bash
export PATH=/bin:/usr/bin:/usr/local/bin

data=($@)
highOrLow=${data[0]}
keyword=${data[1]}
url=${data[2]}
value=${data[3]}
mean=${data[4]}

echo "<a href=$url>$keyword</a> $highOrLow alarm: $value (mean: $mean)" | mail -s "OtiNanai alarm" yourmail@someprovider.com

