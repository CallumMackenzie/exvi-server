/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public class AWSDynamoDB extends DynamoDB implements DocumentDatabase {

    public AWSDynamoDB(AmazonDynamoDB db) {
        super(db);
    }

    public AWSDynamoDB() {
        this(AmazonDynamoDBClientBuilder
                .standard()
                .withRegion(Regions.US_EAST_2)
                .build());
    }
}
