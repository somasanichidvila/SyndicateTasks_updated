package com.task06;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.events.DynamoDbTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "audit_producer",
		roleName = "audit_producer-role",
		isPublishVersion = false,
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@DynamoDbTriggerEventSource(
		targetTable = "Configuration",
		batchSize = 1
)
@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "target_table", value = "${target_table}")
})
public class AuditProducer implements RequestHandler<DynamodbEvent, String> {
	private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
	private final DynamoDB dynamo = new DynamoDB(client);
	private final String DYNAMODB_TABLE = System.getenv("target_table");
	private final Table table = dynamo.getTable(DYNAMODB_TABLE);
	@Override
	public String handleRequest(DynamodbEvent event, Context context) {

		for(DynamodbEvent.DynamodbStreamRecord records: event.getRecords()){
			String eventN=records.getEventName();
			if(eventN.equals("INSERT")){
				addDataToAuditTable(records.getDynamodb().getNewImage());
				break;
			}
			else if(eventN.equals("MODIFY")){
				modifyDataToAuditTable(records.getDynamodb().getNewImage(),records.getDynamodb().getOldImage());

			}
			else{
				break;
			}
		}
		return "";
	}

	private void modifyDataToAuditTable(Map<String, AttributeValue> newImage, Map<String, AttributeValue> oldImage) {
		String key = newImage.get("key").getS();
		int oldV = Integer.parseInt(oldImage.get("value").getN());
		int newV = Integer.parseInt(newImage.get("value").getN());

		if(newV!=oldV){
			Item updateItem= new Item()
					.withPrimaryKey("id",UUID.randomUUID().toString())
					.withString("itemKey",key)
					.withString("modificationTime",DateTimeFormatter.ISO_INSTANT
							.format(Instant.now().atOffset(ZoneOffset.UTC)))
					.withString("updatedAttribute","value")
					.withInt("oldValue",oldV)
					.withInt("newValue",newV);
			table.putItem(updateItem);
		}
	}

	private void addDataToAuditTable(Map<String, AttributeValue> newImage) {

		String key=newImage.get("key").getS();
		int v=Integer.parseInt(newImage.get("value").getN());

		Map<String,Object> newValue=new HashMap<>();
		newValue.put("key",key);
		newValue.put("value", v);

		Item auditItem=new Item()
				.withPrimaryKey("id",UUID.randomUUID().toString())
				.withString("itemKey",key)
				.withString("modificationTime",DateTimeFormatter.ISO_INSTANT
						.format(Instant.now().atOffset(ZoneOffset.UTC)))
				.withMap("newValue",newValue);

		table.putItem(auditItem);
	}
}