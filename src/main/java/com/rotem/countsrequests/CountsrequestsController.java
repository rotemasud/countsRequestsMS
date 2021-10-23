package com.rotem.countsrequests;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;


import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


@RestController
public class CountsrequestsController {
    	
    @RequestMapping("/count")
	public String index() {

		final String table_name = "demo";
        String name = "webapp";
        String projection_expression = "countsRequests";
		Integer requestsCounterFromTable;
		Integer currentrequestsCount;
		String requestsCounterResult;
		String jsonResult;
		final AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.defaultClient();


        System.out.format("Retrieving item \"%s\" from \"%s\"\n",name, table_name);

        HashMap<String,AttributeValue> key_to_get =new HashMap<String,AttributeValue>();

        key_to_get.put("Id", new AttributeValue(name));

        GetItemRequest request = null;
    
            request = new GetItemRequest()
                .withKey(key_to_get)
                .withTableName(table_name)
                .withProjectionExpression(projection_expression);

        try {
			//getting the requestsCounter from dynamoDB table
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
	}
}