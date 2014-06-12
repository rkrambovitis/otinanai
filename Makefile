SRC= src
DST= /home/robert/gr/phaistosnetworks/admin/otinanai

all:
	cd $(SRC) && make ;
	mv $(SRC)/*.class $(DST)

clean: 
	find . -type f -name '*.class' | xargs rm -f

