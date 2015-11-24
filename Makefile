SRC= src
CURDIR = $(shell pwd)
CPATH= $(CURDIR)/jars/jedis.jar
DST= $(CURDIR)/gr/phaistosnetworks/admin/otinanai


FLAGS= -Xlint:unchecked -Xlint:deprecation -cp $(CPATH)
SRCFILES= OtiNanai.java SomeRecord.java OtiNanaiListener.java OtiNanaiWeb.java KeyWordTracker.java OtiNanaiTicker.java OtiNanaiCache.java LLString.java RedisTracker.java OtiNanaiNotifier.java NNComparator.java ValueComparator.java OtiNanaiParser.java
JAVAC= /usr/bin/javac

all:
		mkdir -p $(DST)
		cd $(SRC) && $(JAVAC) $(FLAGS) $(SRCFILES) 
		mv $(SRC)/*.class $(DST)

clean: 
	find . -type f -name '*.class' | xargs rm -f

