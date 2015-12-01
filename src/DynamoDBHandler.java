import java.util.ArrayList;
import java.util.Iterator;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableCollection;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

public class DynamoDBHandler {
	
	public static DynamoDB createDynamoDB(AWSCredentials credentials) {
		DynamoDB dynamoDB;
		AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(credentials);
		dynamoDBClient.setRegion(Region.getRegion(Regions.US_EAST_1)); 
		dynamoDB = new DynamoDB(dynamoDBClient);
		return dynamoDB;
	}
	
	public static void createTable(DynamoDB dynamoDB, String tableName) {
		System.out.println("#Create a table in DynamoDB.#");
		
			try {
			
				ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
				attributeDefinitions.add(new AttributeDefinition()
					.withAttributeName("Id")
					.withAttributeType("N"));

				ArrayList<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
				keySchema.add(new KeySchemaElement()
					.withAttributeName("Id")
					.withKeyType(KeyType.HASH));

				CreateTableRequest request = new CreateTableRequest()
					.withTableName(tableName)
					.withKeySchema(keySchema)
					.withAttributeDefinitions(attributeDefinitions)
					.withProvisionedThroughput(new ProvisionedThroughput()
						.withReadCapacityUnits(5L)
						.withWriteCapacityUnits(6L));

				System.out.println("Issuing CreateTable request for " + tableName);
				Table table = dynamoDB.createTable(request);

				System.out.println("Waiting for " + tableName
					+ " to be created...this may take a while...");
				table.waitForActive();
				
				readTable(dynamoDB, tableName);
			
			} catch (ResourceInUseException e1) {
				System.err.println("Table " + tableName + " already exists.");
				readTable(dynamoDB, tableName);
			} catch (Exception e2) {
				System.err.println("CreateTable request failed for " + tableName);
				System.err.println(e2);
			}
	}	
	
	public static void listingTables(DynamoDB dynamoDB) throws Exception{
		System.out.println("#Listing Tables.#");

		TableCollection<ListTablesResult> tables = dynamoDB.listTables();
		Iterator<Table> iterator = tables.iterator();

		System.out.println("Listing table names");

		while (iterator.hasNext()) {
			Table table = iterator.next();
			System.out.println(table.getTableName());
		}		
	}
	
	public void deleteTable(DynamoDB dynamoDB, String tableName) {
		Table table = dynamoDB.getTable(tableName);
		try {
			System.out.println("Issuing DeleteTable request for " + tableName);
			table.delete();

			System.out.println("Waiting for " + tableName
				+ " to be deleted...this may take a while...");

			table.waitForDelete();
		} catch (Exception e) {
			System.err.println("DeleteTable request failed for " + tableName);
			System.err.println(e.getMessage());
		}
	}
   
	public static void readTable(DynamoDB dynamoDB, String tableName) {
		System.out.println("#Read a table in DynamoDB.");
		System.out.println("Describing " + tableName);

		TableDescription tableDescription = dynamoDB.getTable(tableName).describe();
		System.out.format("Name: %s:\n" + "Status: %s \n"
				+ "Provisioned Throughput (read capacity units/sec): %d \n"
				+ "Provisioned Throughput (write capacity units/sec): %d \n",
		tableDescription.getTableName(), 
		tableDescription.getTableStatus(), 
		tableDescription.getProvisionedThroughput().getReadCapacityUnits(),
		tableDescription.getProvisionedThroughput().getWriteCapacityUnits());	
	}
	
	public static void updateTable(DynamoDB dynamoDB, String tableName, ProvisionedThroughput provisionedThroughput) {
		System.out.println("#UpdateTable in DynamoDB.#");
		Table table = dynamoDB.getTable(tableName);
		System.out.println("Modifying provisioned throughput for " + tableName);

		try {
			table.updateTable(provisionedThroughput);
			table.waitForActive();
			
		} catch (Exception e) {
			System.err.println("UpdateTable request failed for " + tableName);
			System.err.println(e.getMessage());
		}			
	}
}
