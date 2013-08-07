FLAGS= -Xlint:unchecked -Xlint:deprecation
SRCFILES= OtiNanai.java SomeRecord.java OtiNanaiListener.java OtiNanaiCommander.java OtiNanaiWeb.java OtiNanaiProcessor.java KeyWordTracker.java OtiNanaiTicker.java OtiNanaiMemory.java OtiNanaiCache.java
JAVAC= /usr/bin/javac

all:
	$(JAVAC) $(FLAGS) $(SRCFILES)

clean: 
	rm -f ./*.class */*.class

clgen:
	rm -f generated/*

gen: 
	xjc Config.xsd 
