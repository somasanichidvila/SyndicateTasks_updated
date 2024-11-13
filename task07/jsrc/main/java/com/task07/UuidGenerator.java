package com.task07;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.events.RuleEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;

import java.io.*;
import java.time.Instant;
import java.util.*;

@LambdaHandler(lambdaName = "uuid_generator",
		roleName = "uuid_generator-role",
		isPublishVersion = true,
		aliasName = "learn",
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "target_bucket", value = "${target_bucket}")
})
@RuleEventSource(
		targetRule = "uuid_trigger"
)
@DependsOn(
		name = "uuid_trigger",
		resourceType = ResourceType.CLOUDWATCH_RULE
)
public class UuidGenerator implements RequestHandler<ScheduledEvent, Map<String, Object>> {

	private final AmazonS3 s3Client=AmazonS3Client.builder().withRegion("eu-central-1").build();
	private final String BUCKET_NAME = System.getenv("target_bucket");


	@Override
	public Map<String, Object> handleRequest(ScheduledEvent event, Context context) {
		String key = Instant.now().toString();
		List<String> ids = new ArrayList<>();
		for (int a = 0; a < 10; a++) {
			ids.add(UUID.randomUUID().toString());
		}
		String content = "{\n  \"ids\": [\n    \"" + String.join("\",\n    \"", ids) + "\"\n  ]\n}";
		File files = new File("/tmp/AWS.txt");
		try (FileWriter writer_file = new FileWriter(files)) {
			writer_file.write(content);
		} catch (IOException e) {
			e.printStackTrace();
			Map<String, Object> Result = new HashMap<>();
			Result.put("statusCode", 500);
			Result.put("body", "Error writing to file: " + e.getMessage());
			return Result;
		}
		s3Client.putObject(BUCKET_NAME,key,files);
		Map<String, Object> result = new HashMap<>();
		result.put("statusCode", 200);
		result.put("body", "UUIDs generated and stored in S3 bucket");

		return result;
	}
}