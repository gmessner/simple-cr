# Use the latest Alpine OpenJDK 8 image
FROM openjdk:8-alpine

LABEL maintainer "greg@messners.com"

ENV SIMPLE_CR_DIR /usr/local/simple-cr
COPY simple-cr/ $SIMPLE_CR_DIR/

RUN mkdir $SIMPLE_CR_DIR/dbdata
    
VOLUME $SIMPLE_CR_DIR/dbdata
EXPOSE 8080
WORKDIR $SIMPLE_CR_DIR

CMD ["/usr/local/simple-cr/bin/simple-cr"]
