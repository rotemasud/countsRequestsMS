package com.rotem.countsrequests;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;

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
        String name = "webapp";
        String projection_expression = "countsRequests";
		Integer requestsCounterFromTable;
		Integer currentrequestsCount;
		String requestsCounterResult;
		GetItemRequest request = null;
		String jsonResult;
		final AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.defaultClient();
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

			if (returned_item != null) {
                Set<String> keys = returned_item.keySet();
				String key = keys.iterator().next();
				requestsCounterResult = returned_item.get(key).toString();
				System.out.println(requestsCounterResult);
				JSONObject jsonObj = new JSONObject(requestsCounterResult);
				requestsCounterFromTable = Integer.valueOf(jsonObj.getInt("N"));
				System.out.println(requestsCounterFromTable);
				currentrequestsCount=requestsCounterFromTable + 1;

				//updating the requestsCounter in dynamoDB table
				System.out.format("Updating \"%s\" in %s\n", name, table_name);
				HashMap<String,AttributeValue> item_key = new HashMap<String,AttributeValue>();
				item_key.put("Id", new AttributeValue(name));
				HashMap<String,AttributeValueUpdate> updated_values =new HashMap<String,AttributeValueUpdate>();
				updated_values.put("countsRequests", new AttributeValueUpdate(new AttributeValue().withN(currentrequestsCount.toString()), AttributeAction.PUT));
				ddb.updateItem(table_name, item_key, updated_values);
				jsonResult = new JSONObject().put("count", currentrequestsCount).toString();

            } else {
				// this is the first request setting to to 1 in the dynamoDB table
				HashMap<String,AttributeValue> item_values =new HashMap<String,AttributeValue>();
				item_values.put("Id",new AttributeValue(name));
				item_values.put("countsRequests",new AttributeValue().withN("1"));
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