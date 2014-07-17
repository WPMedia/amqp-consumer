package com.washingtonpost.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.*;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Alan on 6/27/14.
 */
public class Consumer {
    private static final String EXCHANGE_NAME = "rumExchange";
    private static Logger logger = Logger.getLogger(Consumer.class.getName());

    public static void main(String[] argv) throws IOException, InterruptedException {


        //read property list
        String filename = argv[0];

        ObjectMapper mapper = new ObjectMapper();

        Property prop;

        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            prop = mapper.readValue(br, Property.class);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        /* First, we'll set up the logger */
        // We're going to try to set log level as a command line option
        logger.setLevel(Level.parse(prop.getLogLevel()));
        logger.addHandler(new ConsoleHandler());

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
                prop.getMongoDatabase(),
                prop.getMongoPassword().toCharArray());
        MongoClient mongoClient = new MongoClient(serverAddress, Arrays.asList(credential), clientOptions);
        DB db = mongoClient.getDB(prop.getMongoDatabase());
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
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, EXCHANGE_NAME, "");
        logger.info("[*] Waiting for messages. To exit press CTRL+C");

        channel.basicQos(1);

        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, false, consumer);


        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());

            logger.info(" [x] Received '" + message + "'");

            BasicDBObject doc = new BasicDBObject();
            try {

                String[] tokens = message.split("##");
                for (String token : tokens) {
                    String[] pairs = token.split("::");
                    if (pairs.length == 1) {
                        doc.put(pairs[0], null);
                        continue;
                    }
                    if (pairs[0].equals("rum")) {
                        //rum data should come first!
                        String decoded = URLDecoder.decode(pairs[1], "UTF-8");
                        doc = (BasicDBObject) JSON.parse(decoded);
                    } else if (pairs[0].equals("createdAt")) {
                        try {
                            DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
                            Date date = format.parse(pairs[1]);
                            doc.put(pairs[0], date);
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "Date parsing exception", e);
                        }
                    } else {
                        doc.put(pairs[0], pairs[1]);
                    }
                }

            } catch (UnsupportedEncodingException uee) {
                logger.log(Level.SEVERE, "UnsupportedEncodingException: ", uee);
            } catch (JSONParseException jpe) {
                logger.log(Level.SEVERE, "JSONParseException", jpe);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Unexpected Error", e);
            }

            if (doc != null) {
                coll.insert(doc);
                logger.info(" [x] Inserted to MongoDB: " + doc.toString());
            } else {
                logger.info(" [x] No json data found");
            }

            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }


    }
}
