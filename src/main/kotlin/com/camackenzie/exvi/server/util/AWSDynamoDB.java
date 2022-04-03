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
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.camackenzie.exvi.server.database.DatabaseEntry;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public class AWSDynamoDB extends DynamoDB implements DocumentDatabase {

    private final Gson gson = new Gson();

    public AWSDynamoDB(AmazonDynamoDB db) {
        super(db);
    }

    public AWSDynamoDB() {
        this(AmazonDynamoDBClientBuilder
                .standard()
                .withRegion(Regions.US_EAST_2)
                .build());
    }

    @NotNull
    @Override
    public Gson getGson() {
        return gson;
    }
}
