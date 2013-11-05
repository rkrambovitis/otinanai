#!/bin/bash
export PATH=/bin:/sbin:/usr/bin:/usr/sbin

cd /home/system/Tools/OtiNanai 
#java -Xms16m -Xmx1024m OtiNanai -lf /tmp/otinanai.log -ll fine -wt 3 -lp 9876 -wp 9876 -ct 5 -ci 10 -al 3600 
java -Xms16m -Xmx1024m -cp jars/riak-client-1.4.1.jar:OtiNanai gr.phaistosnetworks.admin.otinanai.OtiNanai

