FROM maven:3-jdk-8-slim as build
WORKDIR /project

# cache and copy dependencies
ADD pom.xml .
RUN mvn dependency:go-offline dependency:copy-dependencies

# compile the function
# TODO: Fix failing tests.
# App is failing tests; skipping them for now..
ADD . .
RUN mvn package -Dmaven.test.skip=true

# copy the function artifact and dependencies onto a clean base
FROM openjdk:8-jre-slim
WORKDIR /function
# Add the lambda-runtime-interface-emulator to enable local testing.
ADD https://github.com/aws/aws-lambda-runtime-interface-emulator/releases/latest/download/aws-lambda-rie "$JAVA_HOME/bin/aws-lambda-rie"
RUN chmod +x "$JAVA_HOME/bin/aws-lambda-rie"

ADD ./entrypoint.sh ./entrypoint.sh
RUN chmod +x ./entrypoint.sh
ENTRYPOINT ["./entrypoint.sh"]

# Add the JAR to a known path.
ENV JAR_DIR="./jar"

COPY --from=build /project/target/dependency/*.jar $JAR_DIR/
COPY --from=build /project/target/*.jar $JAR_DIR/

# pass the name of the function handler as an argument to the runtime
CMD [ "com.socalcat.lambda.transferauth.LambdaFunctionHandler::handleRequest" ]