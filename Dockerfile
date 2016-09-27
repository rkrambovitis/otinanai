FROM openjdk:alpine

RUN mkdir -p /otinanai/gr/phaistosnetworks/admin/otinanai
WORKDIR /otinanai

ADD src src/
ADD web web/
ADD jars jars/
ADD run.sh ./

RUN javac -d . -cp .:jars/* src/*.java

$ENTRYPOINT ["java","-Xms64m","-Xmx512m","-cp",".:jars/*","gr.phaistosnetworks.admin.otinanai.OtiNanai","-lf","/dev/null","-rh","redis","-notify","/bin/echo"]

#-as 120 -acs 3 -ll info -gpp 60 -notify /mnt/TERAS/otinanai/otinanai_scripts/notify -al 21600 -rh 192.168.10.235 -url https://otinanai.phaistosnetworks.gr -tick 120 -wt 20

