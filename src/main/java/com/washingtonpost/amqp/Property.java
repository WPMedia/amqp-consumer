package com.washingtonpost.amqp;

/**
 *  * Created by wenzhou on 7/11/14.
 *   */
public class Property {
    private String MongoIP;
    private String MongoSource;
    private String MongoUser;
    private String MongoPassword;
    private String MongoColl;
    private String AMQPIP;
    private String AMQPUser;
    private String AMQPPassword;

    public String getMongoIP() {
        return MongoIP;
    }

    public void setMongoIP(String mongoIP) {
        MongoIP = mongoIP;
    }

    public String getMongoSource() {
        return MongoSource;
    }

    public void setMongoSource(String mongoSource) {
        MongoSource = mongoSource;
    }

    public String getMongoUser() {
        return MongoUser;
    }

    public void setMongoUser(String mongoUser) {
        MongoUser = mongoUser;
    }

    public String getMongoPassword() {
        return MongoPassword;
    }

    public void setMongoPassword(String mongoPassword) {
        MongoPassword = mongoPassword;
    }

    public String getMongoColl() {
        return MongoColl;
    }

    public void setMongoColl(String mongoColl) {
        MongoColl = mongoColl;
    }

    public String getAMQPIP() {
        return AMQPIP;
    }

    public void setAMQPIP(String AMQPIP) {
        this.AMQPIP = AMQPIP;
    }

    public String getAMQPUser() {
        return AMQPUser;
    }

    public void setAMQPUser(String AMQPUser) {
        this.AMQPUser = AMQPUser;
    }

    public String getAMQPPassword() {
        return AMQPPassword;
    }

    public void setAMQPPassword(String AMQPPassword) {
        this.AMQPPassword = AMQPPassword;
    }
}

