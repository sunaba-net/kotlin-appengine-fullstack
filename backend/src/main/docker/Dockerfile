FROM openjdk:13-alpine
COPY . /usr/app
WORKDIR /usr/app
CMD ["java", "-cp", "./*:./libs/*", "{{mainClassName}}"]