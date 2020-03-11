FROM openjdk:13-alpine
COPY . /usr/src/myapp
WORKDIR /usr/src/myapp
CMD ["java", "-cp", "./*:./libs/*", "{{mainClassName}}"]