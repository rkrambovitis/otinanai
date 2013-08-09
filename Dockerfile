FROM base
MAINTAINER Robert J. Krambovitis "Robert@split.gr"

RUN apt-get update #09 Aug 2013
RUN apt-get upgrade -y

RUN apt-get install -y openjdk-7-jre

RUN mkdir -p /opt/Cascade
ADD . /opt/Cascade

ENTRYPOINT ["/opt/Cascade"]

CMD ["/usr/bin"]
