FROM ubuntu:20.04

RUN apt-get update && apt-get install -y openjdk-11-jdk openjfx && apt-get clean
RUN useradd -ms /bin/bash app
COPY --chown=app . /home/app/kotcity
WORKDIR /home/app/kotcity
VOLUME /home/app/kotcity/.gradle
ENV JAVA_OPTS '-Xmx512m'
RUN ./gradlew build
