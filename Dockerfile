FROM ubuntu:16.04

RUN apt-get update && apt-get install -y openjdk-8-jdk openjfx && apt-get clean
RUN useradd -ms /bin/bash app
COPY . /home/app/kotcity
RUN chown -Rv app:app /home/app/kotcity
WORKDIR /home/app/kotcity
VOLUME /home/app/kotcity/.gradle
RUN ./gradlew build
