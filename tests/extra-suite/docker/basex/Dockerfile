# Created by Norm Walsh for running the XProc test suite http tests.
# Note: if you change the version of httpd in this container, you
# may also need to change the configuration and other setup files
# mounted in the container (see ../docker-compose.yml)
FROM debian:bookworm

RUN apt-get update -y
RUN apt-get install -y openjdk-17-jdk
RUN apt-get install -y curl zip

CMD ["/bin/bash"]
