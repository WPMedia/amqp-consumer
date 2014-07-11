package com.washingtonpost.amqp;

import com.mongodb.*;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.sun.corba.se.spi.activation.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.text.DateFormat;

import com.google.gson.Gson;
/**
 * Created by Alan on 6/27/14.
 */
public class Consumer {

    private static final String EXCHANGE_NAME = "rumExchange";

    public static void main(String[] argv) throws IOException, InterruptedException {
	//read property list
	Gson gson = new Gson();
	Property prop;
	try{
		BufferedReader br = new BufferedReader(new FileReader(argv[0]));
		prop = gson.fromJson(br, Property.class);
	}
	catch(IOException e){
		e.printStackTrace();
		return;
	}

        /* MongoDB Connecty bits */
        MongoClientOptions clientOptions = new MongoClientOptions.Builder()
                .connectionsPerHost(25)
                .socketTimeout(45000)
                .threadsAllowedToBlockForConnectionMultiplier(10)
                .readPreference(ReadPreference.secondaryPreferred(new BasicDBObject("type", "highIO")))
                .build();
        ServerAddress serverAddress = new ServerAddress(prop.getMongoIP());
        MongoCredential credential = MongoCredential.createMongoCRCredential(
		prop.getMongoUser(), 
		prop.getMongoSource(), 
		prop.getMongoPassword().toCharArray());
        MongoClient mongoClient = new MongoClient(serverAddress, Arrays.asList(credential), clientOptions);
        DB db = mongoClient.getDB(prop.getMongoSource());
        DBCollection coll = db.getCollection(prop.getMongoColl());

        /* RabbitMQ Connecty Bits */
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(prop.getAMQPIP());
        factory.setUsername(prop.getAMQPUser());
        factory.setPassword(prop.getAMQPPassword());
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        String queueName = "rumQueue";
        channel.queueBind(queueName, EXCHANGE_NAME, "");
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        channel.basicQos(1);

        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, false, consumer);

        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());

            System.out.println(" [x] Received '" + message + "'");

            BasicDBObject doc = null;
            String decoded = null;
            try {
                String[] tokens = message.split("DELIMITER");
                //decode
                decoded = URLDecoder.decode(tokens[0], "UTF-8");
                //parse to json
                doc = (BasicDBObject) JSON.parse(decoded);
		//parse date
		DateFormat format = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss");
		Date date = format.parse(tokens[3]);
                doc = doc.append("referer", tokens[1]).append("ua", tokens[2]).append("createdAt", date).append("clientIP", tokens[4]);
            } catch (UnsupportedEncodingException uee) {
                System.err.println("ERROR: Character decoding error.");
                uee.printStackTrace();
            } catch (JSONParseException jpe) {
                System.err.println("ERROR: Not valid JSON");
                jpe.printStackTrace();
            } catch (Exception e) {
                System.err.println("ERROR");
            }

            if (doc != null) {
                coll.insert(doc);
                System.out.println(" [x] Inserted to MongoDB: " + doc.toString());
            } else {
                System.out.println(" [x] No json data found");
            }

            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }


    }
}
