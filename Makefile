SRC= src
DST= home/system/Tools/OtiNanai

all:
	cd $(SRC) && make ;
	mv $(SRC)/*.class $(DST)

clean: 
	find . -type f -name '*.class' | xargs rm -f

