package com.task08;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.open_meteo.OpenMeteoAPI;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.Architecture;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;



@LambdaHandler(
		lambdaName = "api_handler",
		roleName = "api_handler-role",
		aliasName = "${lambdas_alias_name}",
		layers = {"api-layer"},
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@LambdaLayer(
		layerName = "api-layer",
		libraries = {"lib/open-meteo-1.0.1.jar"},
		runtime = DeploymentRuntime.JAVA17,
		architectures = {Architecture.ARM64},
		artifactExtension = ArtifactExtension.ZIP
)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED
)
public class ApiHandler implements RequestHandler<Object, String> {

	public String handleRequest(Object req, Context content) {
		OpenMeteoAPI api_input= new OpenMeteoAPI();
		try {
			return api_input.getWeatherForecast();
		} catch (RuntimeException e) {
			return e.getMessage();
		}

	}
}