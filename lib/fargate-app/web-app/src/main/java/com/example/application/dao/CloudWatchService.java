package com.example.application.dao;
import java.net.URI;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import com.example.application.Toolkit;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsResponse;

@Component
@VaadinSessionScope
public class CloudWatchService {
	private final static Logger logger = LogManager.getLogger(CloudWatchService.class);
	private String username = null;
	private String logStream;
	private String cloudWatchToken = null;
	private final String uuid = UUID.randomUUID().toString();
	private static final String region = System.getenv("AWS_REGION");
	private static Region REGION = Region.of(region);

	public CloudWatchService() {
		Toolkit.init();
	}

	private void publishSuccessfulLoginEvent() {
		String ipAddress = VaadinSession.getCurrent().getBrowser().getAddress();
		String event = logStream + " CONNECTED SourceIP=" + ipAddress + " User=" + username + " Interface=Web";
		publishLogEvent(event);
	}

	
	public static void createLogGroupIfNotExists() {
		DescribeLogGroupsRequest describeLogGroupsRequest = DescribeLogGroupsRequest.builder().logGroupNamePrefix(Toolkit.TRANSFER_CLOUDWATCH_LOGGROUP_NAME).build();
		CloudWatchLogsClient c = CloudWatchLogsClient
						.builder()
						.endpointOverride(URI.create(Toolkit.CLOUDWATCH_VPC_ENDPOINT))
						.region(REGION)
						.build();

				
				DescribeLogGroupsResponse describeLogGroupsResponse = 
						c.describeLogGroups(describeLogGroupsRequest);
				
				
				int logGroupCount = describeLogGroupsResponse.logGroups().size();
				if (logGroupCount == 0) {
					logger.info("Creating log group " + Toolkit.TRANSFER_CLOUDWATCH_LOGGROUP_NAME);
					CreateLogGroupRequest createLogGroupRequest = CreateLogGroupRequest.builder()
						.logGroupName(Toolkit.TRANSFER_CLOUDWATCH_LOGGROUP_NAME)
						.build();
					c.createLogGroup(createLogGroupRequest);
				}
	}
	
	public void publishLogEvent(String details) {
		CloudWatchLogsClient c = CloudWatchLogsClient
				.builder()
				.endpointOverride(URI.create(Toolkit.CLOUDWATCH_VPC_ENDPOINT))
				.region(REGION)
				.build();

		InputLogEvent inputLogEvent = InputLogEvent.builder()
				.message(details)
				.timestamp(System.currentTimeMillis())
				.build();

		PutLogEventsRequest putLogEventsRequest = null;

		try {
			putLogEventsRequest = PutLogEventsRequest.builder()
					.logGroupName(Toolkit.TRANSFER_CLOUDWATCH_LOGGROUP_NAME)
					.logStreamName(logStream)
					.logEvents(inputLogEvent)
					.sequenceToken(cloudWatchToken)
					.build();
			PutLogEventsResponse response = c.putLogEvents(putLogEventsRequest);
			cloudWatchToken = response.nextSequenceToken();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	public void init(String username) {
		this.username = username;
		this.cloudWatchToken = null;
		createLogGroupIfNotExists();
		createLogStream();
		publishSuccessfulLoginEvent();
	}

	private void createLogStream() {
		CloudWatchLogsClient c = CloudWatchLogsClient.builder().region(REGION).build();
		String random = UUID.randomUUID().toString();
		random = random.replaceAll("-", "");
		random = random.substring(0, 16);
		this.logStream = username + "." + random;

		CreateLogStreamRequest k = CreateLogStreamRequest.builder()
				.logGroupName(Toolkit.TRANSFER_CLOUDWATCH_LOGGROUP_NAME)
				.logStreamName(logStream)
				.build();
		c.createLogStream(k);

	}

	public String getLogstream() {
		return logStream;
	}

	public String getCloudWatchToken() {
		return cloudWatchToken;
	}

	public void setCloudWatchToken(String cloudWatchToken) {
		this.cloudWatchToken = cloudWatchToken;
	}

	public String getUuid() {
		return uuid;
	}

}
