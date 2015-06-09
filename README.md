# otinanai #

## Description ##
OtiNanai is a Simple graphing tool, designed to graph anything.  
The main idea is that you can chuck anything at it, and then easily find what you are looking for.  
Also, it's meant to run with very little i/o + system requirements.  
The search bar is pretty simple, and makes it easy to narrow down on something,  
but at the same time, allows you to browse data you didn't recall you were graphing.  
All in all, it makes finding patterns and correlations relatively simple *and* it doesn't require a cluster of supercomputers to run.

## Features ##
+ Multiple data types (gauge, sum, freq, counter)
+ SUM option allows you to easily graph apache traffic per virtualhost.
+ Redis backend: Lightweight, in memory, so you don't need umpteen spinning disks for decent performance.
+ js front end: Browser hog rather than server hog :)
+ Automatic spike handling. No need to clear rrd spikes just because you restarted something :)
+ Multiple metrics per graph. Mean, avg, 99th, 95th, min, max all within the timeframe you choose.
+ Relevant graph viewport. By default spikes are outside graphing area, making graphs relevant.
+ Multiple output visualizations (merged, stacked, gauge or individual)
+ Automatic spike and valley detection and notification
+ Graphs generated on the fly, no need to set up in advance.
+ No fancy queries or programming needed. Just pipe it data, and type keywords in web field.

## Limitations ##
+ Persistence depends on redis with all it's pros and cons. So don't rely on this to store critical data.

## System Requirements ##
+ java 7+
+ redis

## HOWTO build ##
1. $ git clone https://bitbucket.org/robert_krambovitis/otinanai.git
2. cd otinanai
3. $ make
4. sudo apt-get install redis-server

## HOWTO run ##
$ `java -cp jars/jedis.jar:. gr.phaistosnetworks.admin.otinanai.OtiNanai -lf out.log`


## Getting data in ##
+ Frequency (events / sec) - i.e. tail log and graph errors
	* $ `echo key.word > /dev/udp/127.0.0.1/9876`

+ Gauge (mean value) - i.e. graph mem usage of process
	* $ `echo key.word {value} > /dev/udp/127.0.0.1/9876`

+ Counter (rate of change / sec) - i.e. graph network activity from snmp counter
	* $ `echo key.word {value} COUNTER > /dev/udp/127.0.0.1/9876`

+ Sum (sum of values / sec) - i.e. graph virtulhost traffic
	* $ `echo key.word {value} SUM > /dev/udp/127.0.0.1/9876`

+ Events - i.e. mark something on graphs
	* $ `echo eventmarker something important happened > /dev/udp/127.0.0.1/9876`
	* $ ``echo eventmarker `date +%s -d "today 12:30"` older event > /dev/udp/127.0.0.1/9876``

## Getting data out ##
+ Just point your browser to 127.0.0.1:9876 and type part of a keyword in the search field.

### Web Input Switches ###
+ Use the following to refine the output
	* `^chars` (starts with chars)
	* `chars$` (ends with chars)
	* `-chars` (exclude keywords that contain chars)
	* `+chars` (exclude keywords that don't contain chars)
	* `@timerange (@24 or @2d or @3d-5d)`
	* `--delete` (delete data of matching keywords
	* `--gauge|--dial` (draw as gauges instead of line graphs)
	* `--sa|--show` (show all matching graphs, i.e. override the max-per-page setting)
	* `--nc|--no-cache` (ignore cache)
	* `--m|--merge|--combine` (merge all graphs into one. Beware, looks like crap)
	* `--stack` (stack graphs)
	* `--alarms|--alerts` (show only matching keywords in "alarm" state)
	* `#maxMergeCount` (change how many graphs are merged per graph, sorted by 99%.)
	* `--limit <number>` (Limit to first <number> graphs)
	* `--nb|--no-bar|--ns|--no-search` (Do not show the search bar - for embedding)
	* `--units <somethings>` (Set the matching keywords units to <somethings>)
	* `--multiplier number` (Set the matching keywords multiplier to <number> (for converting bytes etc))
	* `--noalarm|--na` (disable alarm for matching 
	* `--enable-alarm|--enalarm|--ea` (enable alarm for matching keywords) (alarms are ON by default)

+ Examples:
	* `.com$ +mysite`
	* `host -subdomain --merge --no-cache #10`
	* `crapdata --delete`
	* `dataroom.temperature --gauge`
	* `dataroom.temperature --units celcius`
	* `some.keyword @1d-3d`
	* `apache.vhosts --merge --limit 6`
	* `^snmp --noalarm`

## Command line arguments ##
	-help 	: This output
	-wp <webPort>         : Web Interface Port (default: 9876)
	-lp <listenerPort>    : UDP listener Port (default: 9876)
	-url <webUrl>         : Web Url (for links in notifications) (default: host:port)
	-wt <webThreads>      : No Idea, probably unused
	-ct <cacheTime>       : How long (seconds) to cache generated page (default: 120)
	-ci <cacheItems>      : How many pages to store in cache (default: 50)
	-al <alarmLife>       : How long (seconds) an alarm state remains (default: 86400)
	-as <alarmSamples>    : Minimum samples before considering for alarm (default: 20)
	-atl <lowAlarmThreshold>   : Low alarm threshold multiplier (how many times below mean is alarm) (default: 10)
	-ath <highAlarmThreshold>  : High alarm threshold multiplier (how many times above mean is alarm) (default: 6)
	-acs <alarmConsecutiveSamples>    : How many consecutive samples above threshold trigger alarm state (default: 3)
	-notify <notifyScript>            : Script to use for alarms (default: /tmp/otinanai_notifier)
	-gpp <graphsPerPage>  : Max graphs per page (default: 30)
	-tick <tickInterval>  : Every how often (seconds) does the ticker run (add new samples, aggregate old) (default: 60)
	-s1samples <step1Samples>         : Samples to keep before aggregating oldest (default: 1440)
	-s1agg <step1SamplesToAggregate>  : Samples to aggregate when sample count exceeded (default: 10)
	-s2samples <step2Samples>         : Aggregated samples to keep before further aggregating oldest (default: 2880)
	-s2agg <step2SamplesToAggregate>  : Aggregates samples to further aggregate when count exceeded (default: 6)
	-lf <logFile>         : 
	-ll <logLevel>        : finest, fine, info, config, warning, severe (default: config)
	-rh <redisEndPoint>   : Redis endpoint (default: localhost)
	-rdkwlist <redisKeyWordListName>  : Name of keyword list, useful for more than one instance running on the same redis. (default: existing_keywords_list)
	-rdsvq <redisSavedQueriesList>    : Name of saved queries list for redis. (default: saved_queries_list)
	-rdevtlist <redisEventList>    : Name of event list for redis. (default: OtiNanai_Event_List)
	-rdunitlist <redisUnitList>    : Name of event list for redis. (default: OtiNanai_Event_List)

## Screenshots ##
![Screenshot from 2015-06-09 10:07:09.png](https://bitbucket.org/repo/xyeeMB/images/2390084731-Screenshot%20from%202015-06-09%2010:07:09.png)
![Screenshot from 2015-06-09 10:15:37.png](https://bitbucket.org/repo/xyeeMB/images/69167439-Screenshot%20from%202015-06-09%2010:15:37.png)