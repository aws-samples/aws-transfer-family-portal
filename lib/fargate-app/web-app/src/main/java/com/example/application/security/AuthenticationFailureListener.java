// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.example.application.security;
import java.net.URI;
import java.util.Optional;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;
import com.example.application.Toolkit;
import com.example.application.dao.CloudWatchService;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogStream;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Component
public class AuthenticationFailureListener implements ApplicationListener<AuthenticationFailureBadCredentialsEvent> {
	private final static Logger logger = LogManager.getLogger(AuthenticationFailureListener.class);
	private String ip;
	private static final String region = System.getenv("AWS_REGION");
	private static Region REGION = Region.of(region);

	public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {
		String details = "ERRORS AUTH_FAILURE Method=HTTPS User=" + event.getAuthentication().getPrincipal()
				+ " Message= SourceIP=" + ip;
		// System.out.println("creds="
		// +event.getAuthentication().getCredentials().toString());
		// System.out.println(details);
		publishLogEvent(details);

	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	
	

	
	public void publishLogEvent(String details) {
		CloudWatchService.createLogGroupIfNotExists();
		CloudWatchLogsClient c = CloudWatchLogsClient
				.builder()
				.endpointOverride(URI.create(Toolkit.CLOUDWATCH_VPC_ENDPOINT))
				.region(REGION)
				.build();
		//DescribeLogGroupRequests./
		DescribeLogStreamsRequest request = DescribeLogStreamsRequest.builder()
				.logGroupName(Toolkit.TRANSFER_CLOUDWATCH_LOGGROUP_NAME)
				.logStreamNamePrefix("ERRORS")
				.build();

		DescribeLogStreamsResponse describeLogStreamsResponse = c.describeLogStreams(request);
		Optional<LogStream> logStream = describeLogStreamsResponse
				.logStreams()
				.stream()
				.filter(s -> s.logStreamName().equals("ERRORS"))
				.findFirst();
		// .findFirst().get().uploadSequenceToken();
		String token = null;
		if (logStream.isPresent()) {
			token = logStream.get().uploadSequenceToken();
		}
		else {
			CreateLogStreamRequest createLogStreamRequest = CreateLogStreamRequest.builder()
					.logGroupName(Toolkit.TRANSFER_CLOUDWATCH_LOGGROUP_NAME)
					.logStreamName("ERRORS")
					.build();
			c.createLogStream(createLogStreamRequest);
		}
		InputLogEvent inputLogEvent = InputLogEvent.builder()
				.message(details)
				.timestamp(System.currentTimeMillis())
				.build();

		PutLogEventsRequest putLogEventsRequest = null;

		try {
			putLogEventsRequest = PutLogEventsRequest.builder()
					.logGroupName(Toolkit.TRANSFER_CLOUDWATCH_LOGGROUP_NAME)
					.logStreamName("ERRORS")
					.logEvents(inputLogEvent)
					.sequenceToken(token)
					.build();
			PutLogEventsResponse response = c.putLogEvents(putLogEventsRequest);
			// cloudWatchToken = response.nextSequenceToken();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}
}