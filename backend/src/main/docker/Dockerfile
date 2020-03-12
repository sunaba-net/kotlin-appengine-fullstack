# see https://cloud.google.com/appengine/docs/flexible/custom-runtimes/build#docker
FROM gcr.io/google-appengine/openjdk8
COPY . $APP_DESTINATION
#WORKDIR $APP_DESTINATION なくてもよさそう？
CMD ["java", "-cp", "./*:./libs/*", "{{mainClassName}}"]