
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.Set;
import java.io.IOException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import java.net.URLDecoder;


/**
 * Created by Alan on 6/27/14.
 */
public class amqp_consumer {
    private static final String EXCHANGE_NAME = "rumExchange";

    public static void main(String[] argv) throws IOException, InterruptedException {

        MongoClient mongoclient = new MongoClient("localhost");
        DB db = mongoclient.getDB("mydb");

        DBCollection coll = db.getCollection("testData");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("10.128.136.170");
        factory.setUsername("rum");
        factory.setPassword("rum");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        String queueName="rumQueue";
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
            try{
                //decode
                decoded = URLDecoder.decode(message, "UTF-8");
                //parse to json
                doc = (BasicDBObject)JSON.parse(decoded);
            }
            catch(UnsupportedEncodingException uee){
                System.err.println("ERROR: Character decoding error.");
                uee.printStackTrace();
            }
            catch (JSONParseException jpe){
                System.err.println("ERROR: Not valid JSON");
                jpe.printStackTrace();
            }
            catch(Exception e){
                System.err.println("ERROR");
            }

            if(doc!=null) {
                coll.insert(doc);
                System.out.println(" [x] Inserted to MongoDB: " + decoded);
            }
            else{
                System.out.println(" [x] No json data found");
            }

            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }


    }
}