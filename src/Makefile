FLAGS= -Xlint:unchecked -Xlint:deprecation -cp /home/robert/jars/*.jar
SRCFILES= OtiNanai.java SomeRecord.java OtiNanaiListener.java OtiNanaiWeb.java KeyWordTracker.java OtiNanaiTicker.java OtiNanaiMemory.java OtiNanaiCache.java MemTracker.java RiakTracker.java LLString.java HMStringONM.java
JAVAC= /usr/bin/javac

all:
	$(JAVAC) $(FLAGS) $(SRCFILES)

clean: 
	rm -f ./*.class */*.class

clgen:
	rm -f generated/*

gen: 
	xjc Config.xsd 
