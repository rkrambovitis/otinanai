# otinanai #

## Description ##
OtiNanai is a Simple graphing tool, designed to graph anything.  
The main idea is that you can chuck anything at it, and then easily find what you are looking for.  
Also, it's meant to run with very little i/o + system requirements.  
The search bar is pretty simple, and makes it easy to narrow down on something,  
but at the same time, allows you to browse data you didn't recall you were graphing.  
All in all, it makes finding patterns and correlations relatively simple *and* it doesn't require a cluster of supercomputers to run.

![Screenshot](https://cloud.githubusercontent.com/assets/12841643/14060371/558c2ecc-f36b-11e5-82ca-8339aa412688.png)

## Features ##
+ Multiple data types (gauge, sum, freq, counter)
+ SUM option allows you to easily graph apache traffic per virtualhost.
+ Redis backend: Lightweight, in memory, so you don't need umpteen spinning disks for decent performance.
+ js front end: Browser hog rather than server hog :)
+ Automatic spike handling. No need to clear rrd spikes just because you restarted something :)
+ Multiple metrics per graph. Mean, avg, 99th, min, max all within the timeframe you choose.
+ Relevant graph viewport. By default spikes are outside graphing area, making graphs relevant.
+ For the outliers lovers, percentiles graph has been added.
+ Multiple output visualizations (merged, stacked, gauge, percentiles or individual)
+ Automatic spike and valley detection and notification
+ Graphs generated on the fly, no need to set up in advance.
+ No fancy queries or programming needed. Just pipe it data, and type keywords in web field.

## Limitations ##
+ Persistence depends on redis with all it's pros and cons. So don't rely on this to store critical data.
+ Percentiles may or may not be accurate depending on data aggregation.

## Docker ##
docker run -d --link redis:redis -p 9876:9876 -p 9876:9876/udp otinanai

## System Requirements ##
+ java 7+
+ redis

## HOWTO build ##
1. git clone https://github.com/phaistos-networks/otinanai.git
2. cd otinanai
3. make
4. sudo apt-get install redis-server

## HOWTO run ##
$ `java -cp jars/commons-pool2.jar:jars/jedis.jar:. gr.phaistosnetworks.admin.otinanai.OtiNanai -lf out.log`

## Getting data in ##
<https://github.com/phaistos-networks/otinanai/wiki/Data-Input>

## Graphing ##
<https://github.com/phaistos-networks/otinanai/wiki/Search-Bar-options>


## Command line arguments ##
* `-help` to view command line arguments with explanations.
