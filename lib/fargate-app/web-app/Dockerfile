FROM openjdk:11
WORKDIR /
ADD target/myapp-1.0-SNAPSHOT.jar app.jar
ADD rds-ca-2019-root.pem rds-ca-2019-root.pem
RUN $JAVA_HOME/bin/keytool -import -file rds-ca-2019-root.pem -alias rdscert -cacerts  -storepass changeit -noprompt
RUN useradd -m myuser
USER myuser
EXPOSE 5000
CMD java -jar -Dspring.profiles.active=prod app.jar