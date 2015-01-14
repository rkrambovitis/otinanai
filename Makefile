SRC= src
CPATH= /home/robert/jars/jedis-2.6.1.jar
BASEPATH= /home/robert
DST= $(BASEPATH)/gr/phaistosnetworks/admin/otinanai

FLAGS= -Xlint:unchecked -Xlint:deprecation -cp $(CPATH)
#FLAGS= -Xlint:unchecked -Xlint:deprecation 
SRCFILES= OtiNanai.java SomeRecord.java OtiNanaiListener.java OtiNanaiWeb.java KeyWordTracker.java OtiNanaiTicker.java OtiNanaiCache.java MemTracker.java LLString.java RedisTracker.java OtiNanaiNotifier.java
JAVAC= /usr/bin/javac

all:
		mkdir -p $(DST)
		cd $(SRC) && $(JAVAC) $(FLAGS) $(SRCFILES) 
		mv $(SRC)/*.class $(DST)

clean: 
	find . -type f -name '*.class' | xargs rm -f

