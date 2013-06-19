FLAGS= -Xlint:unchecked -Xlint:deprecation
SRCFILES= OtiNanai.java SomeRecord.java OtiNanaiListener.java OtiNanaiCommander.java
JAVAC= /usr/bin/javac

all:
	$(JAVAC) $(FLAGS) $(SRCFILES)

clean: 
	rm -f ./*.class */*.class

clgen:
	rm -f generated/*

gen: 
	xjc Config.xsd 
