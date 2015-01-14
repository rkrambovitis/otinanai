# otinanai #
Simple graphing tool.

## System Requirements ##
+ java 7+

+ redis (recommended for data retention)
    * By default otinanai just writes to memory, and does not retain anything.

+ jedis ( http://search.maven.org/#browse%7C687224809 )
    * Place into src/jars

## Required js libraries (Place into web dir) ##
+ flot ( http://www.flotcharts.org/ )
    * jquery.flot.crosshair.js
    * jquery.flot.js
    * jquery.flot.resize.js
    * jquery.flot.selection.js
    * jquery.flot.stack.js
    * jquery.flot.stack.min.js
    * jquery.flot.time.js
+ jquery ( http://jquery.com/ )
+ justgage ( http://justgage.com/ )
+ raphael ( http://raphaeljs.com/ )

## HOWTO build ##
1. $ git clone https://github.com/rkrambovitis/otinanai.git
2. Fetch requirements and place into correct directories
3. $ make

## HOWTO run ##
$ java -cp src/jars/jedis-2.6.1.jar:. gr.phaistosnetworks.admin.otinanai.OtiNanai -lf out.log

## Command line arguments ##
	-wp <webPort>         : Web Interface Port (default: 9876)
	-lp <listenerPort>    : UDP listener Port (default: 9876)
	-url <webUrl>         : Web Url (for links in notifications) (default: host:port)
	-wt <webThreads>      : No Idea, probably unused
	-ct <cacheTime>       : How long (seconds) to cache generated page (default: 120)
	-ci <cacheItems>      : How many pages to store in cache (default: 50)
	-al <alarmLife>       : How long (seconds) an alarm state remains (default: 86400)
	-as <alarmSamples>    : Minimum samples before considering for alarm (default: 20)
	-at <alarmThreshold>  : Alarm threshold multiplier (how many times above/below average is an alarm) (default: 3.0)
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
	-redis                : Use redis storage engine (recommended) (default uses volatile memory engine)
	-rh <redisEndPoint>   : Redis endpoint (default: localhost)
	-rdkwlist <redisKeyWordListName>  : Name of keyword list, useful for more than one instance running on the same redis. (default: existing_keywords_list)
	-rdsvq <redisSavedQueriesList>    : Name of saved queries list for redis. (default: saved_queries_list)

## Getting data in ##
+ Frequency (events / sec) - i.e. tail log and graph errors
    * $ echo key.word > /dev/udp/127.0.0.1/9876

+ Gauge (mean value) - i.e. graph mem usage of process
    * $ echo key.word {value} > /dev/udp/127.0.0.1/9876

+ Counter (rate of change / sec) - i.e. graph network activity from snmp counter
    * $ echo key.word {value} COUNTER > /dev/udp/127.0.0.1/9876

+ Sum (sum of values / sec) - i.e. graph virtulhost traffic
    * $ echo key.word {value} SUM > /dev/udp/127.0.0.1/9876

## Getting data out ##
+ Just point your browser to 127.0.0.1:9876 and type part of a keyword in the search field.

### Web Input Switches ###
+ Use the following to refine the output
    * ^chars (starts with chars)
    * chars$ (ends with chars)
    * -chars (exclude keywords that contain chars)
    * +chars (exclude keywords that don't contain chars)
    * @hrs (change time range to hrs - default 24)
    * @+hrs (change start time to hrs back)
    * --delete (delete data of matching keywords
    * --gauge|--dial (draw as gauges instead of line graphs)
    * --sa|--show (show all matching graphs, i.e. override the max-per-page setting)
    * --nc|--no-cache (ignore cache)
    * --m|--merge|--combine (merge all graphs into one. Beware, looks like crap)
    * --ma|--merge-axis|--merge-axes (same as above, but scale data to fit)
    * --alarms|--alerts (show only matching keywords in "alarm" state)
    * --nb|--no-bar|--ns|--no-search (Do not show the search bar - for embedding)

+ Examples:
    * .com$ +mysite 
    * host -subdomain --merge --no-cache
    * crapdata --delete
    * dataroom.temperature --gauge
    * some.keyword @+48 @3