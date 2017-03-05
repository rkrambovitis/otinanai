SRC= src
CURDIR = $(shell pwd)
CPATH= $(CURDIR)/jars/jedis.jar:$(CURDIR)/jars/commons-pool2.jar:$(CURDIR)/jars/protobuf.jar
PACKAGE_DIR= gr/phaistosnetworks/admin/otinanai
DST= $(CURDIR)/$(PACKAGE_DIR)


FLAGS= -Xlint:unchecked -Xlint:deprecation -cp $(CPATH)
SRCFILES= OtiNanai.java SomeRecord.java OtiNanaiListener.java OtiNanaiWeb.java KeyWordTracker.java OtiNanaiTicker.java OtiNanaiCache.java LLString.java RedisTracker.java OtiNanaiNotifier.java NNComparator.java ValueComparator.java OtiNanaiParser.java OtiNanaiProtos.java OtiNanaiHistogram.java
PROTO_FILE= oti_nanai_protos.proto
JAVAC= /usr/bin/javac

all:
	mkdir -p $(DST)
	cd $(SRC) && $(JAVAC) $(FLAGS) $(SRCFILES) 
	mv $(SRC)/*.class $(DST)

proto:
	mkdir -p proto
	protoc --java_out=proto $(SRC)/$(PROTO_FILE)
	mv proto/$(PACKAGE_DIR)/*.java $(SRC)
	rm -r proto

clean: 
	find . -type f -name '*.class' | xargs rm -f

.PHONY: proto clean
