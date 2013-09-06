#!/bin/bash
export PATH=/bin:/sbin:/usr/bin:/usr/sbin

cd /home/system/Tools/OtiNanai 
java -Xms16m -Xmx1024m OtiNanai -lf /tmp/otinanai.log -ll fine -wt 3 -lp 9876 -wp 9876 -ct 5 -ci 10 -al 3600 
