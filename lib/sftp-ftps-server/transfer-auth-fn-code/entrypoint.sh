#!/usr/bin/env bash

HANDLER="$1"

if [ -z "${AWS_LAMBDA_RUNTIME_API}" ]; then
    exec $JAVA_HOME/bin/aws-lambda-rie $JAVA_HOME/bin/java -cp "$JAR_DIR/*" "com.amazonaws.services.lambda.runtime.api.client.AWSLambda" "$HANDLER"
else
    exec $JAVA_HOME/bin/java -cp "$JAR_DIR/*" "com.amazonaws.services.lambda.runtime.api.client.AWSLambda" "$HANDLER"
fi