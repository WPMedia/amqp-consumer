package com.washingtonpost.amqp;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *  * Created by wenzhou on 7/11/14.
 *   */
public class Property {

    @JsonProperty("mongo_ip")
    private String MongoIP;

    @JsonProperty("mongo_database")
    private String MongoDatabase;

    @JsonProperty("mongo_user")
    private String MongoUser;

    @JsonProperty("mongo_password")
    private String MongoPassword;

    @JsonProperty("mongo_collection")
    private String MongoColl;

    @JsonProperty("amqp_ip")
    private String AMQPIP;

    @JsonProperty("amqp_user")
    private String AMQPUser;

    @JsonProperty("amqp_password")
    private String AMQPPassword;

    @JsonProperty("log_level")
    private String logLevel;

    public String getMongoIP() {
        return MongoIP;
    }

    public void setMongoIP(String mongoIP) {
        MongoIP = mongoIP;
    }

    public String getMongoDatabase() {
        return MongoDatabase;
    }

    public void setMongoDatabase(String mongoDatabase) {
        MongoDatabase = mongoDatabase;
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

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }
}

