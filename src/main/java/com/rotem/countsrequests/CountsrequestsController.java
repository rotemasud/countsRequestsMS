package com.rotem.countsrequests;

import java.util.HashMap;
import java.util.Map;


import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.SsmException;


@RestController
public class CountsrequestsController {
    	
    @RequestMapping("/count")
	public String index() {

		final String paraName="table_name";
		String table_name;
        String name = "webserver";
        String projection_expression = "countsRequests";
		Integer requestsCounterFromTable;
		String requestsCounterResult;
		GetItemRequest request = null;
		String jsonResult;
		final AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.defaultClient();
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
		DynamoDB dynamoDB = new DynamoDB(client);
		final SsmClient ssmClient = SsmClient.builder().build();
        HashMap<String,AttributeValue> key_to_get =new HashMap<String,AttributeValue>(); 
		   		
        try {

			// getting the table name from parameter store
			System.out.format("Retrieving value of the key \"%s\" from paramter store\n",paraName);
			GetParameterRequest parameterRequest = GetParameterRequest.builder().name(paraName).build();
			GetParameterResponse parameterResponse = ssmClient.getParameter(parameterRequest);
			table_name = parameterResponse.parameter().value();
            System.out.println("The parameter value is "+ table_name);
			key_to_get.put("Id", new AttributeValue(name));

			//getting the requestsCounter from dynamoDB table
			System.out.format("Retrieving item \"%s\" from \"%s\"\n",name, table_name);
			request = new GetItemRequest().withKey(key_to_get).withTableName(table_name).withProjectionExpression(projection_expression);
			Map<String,AttributeValue> returned_item =ddb.getItem(request).getItem();
			Table table = dynamoDB.getTable(table_name);

			if (returned_item != null) {

			/////atomic counter
			UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey("Id", name)
			.withUpdateExpression("set countsRequests = countsRequests + :val")
			.withValueMap(new ValueMap().withNumber(":val", 1)).withReturnValues(ReturnValue.UPDATED_NEW);
			System.out.println("Incrementing an atomic counter...");
            UpdateItemOutcome outcome = table.updateItem(updateItemSpec);
			requestsCounterResult = outcome.getItem().toJSONPretty();
			System.out.println("UpdateItem succeeded:\n" +requestsCounterResult);
			JSONObject jsonObj = new JSONObject(requestsCounterResult);
			requestsCounterFromTable = Integer.valueOf(jsonObj.getInt(projection_expression));
			jsonResult = new JSONObject().put("count", requestsCounterFromTable).toString();

            } else {
				// this is the first request setting to to 1 in the dynamoDB table
				HashMap<String,AttributeValue> item_values =new HashMap<String,AttributeValue>();
				item_values.put("Id",new AttributeValue(name));
				item_values.put(projection_expression,new AttributeValue().withN("1"));
				ddb.putItem(table_name, item_values);
                System.out.format("No item found with the key %s!\n", name);
				jsonResult = new JSONObject().put("count", 1).toString();
            }

			return jsonResult;

        } catch (AmazonServiceException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getErrorMessage());
        }		
		catch (SsmException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}	
	}
}