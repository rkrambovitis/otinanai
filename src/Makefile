CPATH= /home/robert/jars/jedis-2.6.1.jar
FLAGS= -Xlint:unchecked -Xlint:deprecation -cp $(CPATH)
SRCFILES= OtiNanai.java SomeRecord.java OtiNanaiListener.java OtiNanaiWeb.java KeyWordTracker.java OtiNanaiTicker.java OtiNanaiCache.java MemTracker.java LLString.java RedisTracker.java OtiNanaiNotifier.java
JAVAC= /usr/bin/javac

all:
	$(JAVAC) $(FLAGS) $(SRCFILES)

clean: 
	rm -f ./*.class */*.class

clgen:
	rm -f generated/*

gen: 
	xjc Config.xsd 
