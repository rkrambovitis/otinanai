FROM base
MAINTAINER Robert J. Krambovitis "Robert@split.gr"

RUN apt-get update
RUN apt-get install -y openjdk-7-jre-headless

ADD . /

EXPOSE 80 9876/udp

RUN cd /home/system/Tools/OtiNanai
CMD ["/bin/java OtiNanai -lf /tmp/crap.log "]
