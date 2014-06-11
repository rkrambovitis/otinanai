FROM base
MAINTAINER Robert J. Krambovitis "rkrambovitis@gmail.com"

RUN apt-get update
RUN apt-get install -y openjdk-7-jre-headless

EXPOSE 9876:9876 
EXPOSE 9876:9876/udp

ENV MYDOCKVER 2013-09-03-01
ADD . /
#RUN mkdir -p /tmp/crap
CMD ["startOtiNanai.sh"]
