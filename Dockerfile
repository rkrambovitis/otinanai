FROM base
MAINTAINER Robert J. Krambovitis "Robert@split.gr"

RUN apt-get update
RUN apt-get install -y openjdk-7-jre-headless


EXPOSE 80 9876/udp

ADD . /
RUN mkdir -p /tmp/crap
CMD ["./startOtiNanai.sh"]
