import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Date;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.ConfirmSubscriptionRequest;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.SubscribeRequest;

import org.codehaus.jackson.map.ObjectMapper;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

@ServerEndpoint("/echo")
public class server {
	
	static BlockingQueue<Map<String, String>> messageQueue = new LinkedBlockingQueue<Map<String, String>>();
	
	static class AmazonSNSHandler extends AbstractHandler {

		// Handle HTTP request
		public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException {

			// Scan request into a string
			Scanner scanner = new Scanner(request.getInputStream());
			StringBuilder sb = new StringBuilder();
			while (scanner.hasNextLine()) {
				sb.append(scanner.nextLine());
			}
			
			// Build a message map from the JSON encoded message
			InputStream bytes = new ByteArrayInputStream(sb.toString().getBytes());
			Map<String, String> messageMap = new ObjectMapper().readValue(bytes, Map.class);

			// Enqueue message map for receive loop
			messageQueue.add(messageMap);

			// Set HTTP response
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			((Request) request).setHandled(true);
		}        
	}

	@OnOpen
	public void onOpen(Session session) throws Exception {
		
		System.out.println("A client is connected.");

	}

	@OnMessage
	public void onMessage(String message, Session session) throws Exception {
		try {
			
			DynamoDB dynamoDB = new DynamoDB(new AmazonDynamoDBClient(new ProfileCredentialsProvider()));
			String tableName = "Assignment1";

			Table table = dynamoDB.getTable(tableName);
			String[] messageSplit;
			messageSplit = message.split(" ", 2);

			if (messageSplit[0].equals("SENTIMENT")) {
				
				AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();
				
				// Create a client
				AmazonSNSClient service = new AmazonSNSClient(credentials);
				
				// Create a topic
				CreateTopicRequest createReq = new CreateTopicRequest()
					.withName("test1");
				CreateTopicResult createRes = service.createTopic(createReq);
				
				// Get an HTTP Port
				int port = 9063;
				
				// Create and start HTTP server
				Server server = new Server(port);

				server.setHandler(new AmazonSNSHandler());
				server.start();
				
				System.out.println("http://" + InetAddress.getLocalHost().getHostAddress() + ":" + port);
				// Subscribe to topic
				SubscribeRequest subscribeReq = new SubscribeRequest()
					.withTopicArn(createRes.getTopicArn())
					.withProtocol("http")
					.withEndpoint("http://" + InetAddress.getLocalHost().getHostAddress() + ":" + port);
				service.subscribe(subscribeReq);
				

				for (;;) {

					// Wait for a message from HTTP server
					Map<String, String> messageMap = messageQueue.take();

					// Look for a subscription confirmation Token
					String token = messageMap.get("Token");
					if (token != null) {

						// Confirm subscription
						ConfirmSubscriptionRequest confirmReq = new ConfirmSubscriptionRequest()
							.withTopicArn(createRes.getTopicArn())
							.withToken(token);
						service.confirmSubscription(confirmReq);

						continue;
					}

					// Check for a notification
					String receivedMessage = messageMap.get("Message");
					if (receivedMessage != null) {
						System.out.println("Received message: " + receivedMessage);
						
						String[] receivedMessagePart = receivedMessage.split(" ");
						
						double latitude = Double.parseDouble(receivedMessagePart[0]);
						double longitude = Double.parseDouble(receivedMessagePart[1]);
						int sentiment = 0;
						if (receivedMessagePart[2].equals("neutral")) {
							sentiment = 0;
						} else if (receivedMessagePart[2].equals("negative")) {
							sentiment = -1;
						} else if (receivedMessagePart[2].equals("positive")) {
							sentiment = 1;
						}
						
						String sendMessage = latitude + " " + longitude + " " + sentiment;
						session.getBasicRemote().sendText(sendMessage);
						
					}
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	@OnClose
	public void onClose(Session session){
		System.out.println("The client is closed.");
	}
}
