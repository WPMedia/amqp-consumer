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
import java.net.URLDecoder;
import java.util.Arrays;


/**
 * Created by Alan on 6/27/14.
 */
public class Consumer {

    private static final String EXCHANGE_NAME = "rumExchange";

    public static void main(String[] argv) throws IOException, InterruptedException {

        /* MongoDB Connecty bits */
        MongoClientOptions clientOptions = new MongoClientOptions.Builder()
                .connectionsPerHost(25)
                .socketTimeout(45000)
                .threadsAllowedToBlockForConnectionMultiplier(10)
                .readPreference(ReadPreference.secondaryPreferred(new BasicDBObject("type", "highIO")))
                .build();
        ServerAddress serverAddress = new ServerAddress("10.128.134.151");
        MongoCredential credential = MongoCredential.createMongoCRCredential("rumds_user", "rumds", "xcmhA2kTUWGue5JlAp6R".toCharArray());
        MongoClient mongoClient = new MongoClient(serverAddress, Arrays.asList(credential), clientOptions);
        DB db = mongoClient.getDB("rumds");
        DBCollection coll = db.getCollection("rumetrics");

        /* RabbitMQ Connecty Bits */
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("10.128.136.170");
        factory.setUsername("rum");
        factory.setPassword("rum");
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
                doc = doc.append("referer", tokens[1]).append("ua", tokens[2]).append("createdAt", tokens[3]).append("clientIP", tokens[4]);
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
