package com.task02;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;

import java.util.HashMap;
import java.util.Map;

@LambdaHandler(
    lambdaName = "hello_world",
	roleName = "hello_world-role",
	isPublishVersion = true,
	aliasName = "${lambdas_alias_name}",
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@LambdaUrlConfig(

		authType = AuthType.NONE, // This means no authentication is required to access the Function URL

		invokeMode = InvokeMode.BUFFERED // This can be set to BUFFERED or STREAMING based on your requirements

)
public class HelloWorld implements RequestHandler<Map<String, Object>, Map<String, Object>> {

	@Override
	public Map<String, Object> handleRequest(Map<String, Object> request, Context context) {
		Map<String, Object> requestContext = (Map<String, Object>) request.get("requestContext");
		Map<String, Object> httpContext = (Map<String, Object>) requestContext.get("http");
		String httpMethod = (String) httpContext.get("method");
		String path = (String) httpContext.get("path");

		Map<String, Object> response = new HashMap<>();
		path = path.trim();

		if (path.equals("/hello")) {
			response.put("statusCode", 200);
			response.put("body", "{\"statusCode\": 200, \"message\": \"Hello from Lambda\"}");
		} else {
			response.put("statusCode", 400);
			response.put("body", "{\"statusCode\": 400, \"message\": \"Bad request syntax or unsupported method. Request path: " + path + ". HTTP method: " + httpMethod + "\"}");
		}

		return response;
	}
}
