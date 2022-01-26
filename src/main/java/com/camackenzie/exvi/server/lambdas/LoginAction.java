/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.lambdas;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.camackenzie.exvi.core.api.AccountAccessKeyResult;
import com.camackenzie.exvi.core.api.LoginRequest;
import com.camackenzie.exvi.server.util.AWSDynamoDB;
import com.camackenzie.exvi.server.util.AuthUtils;
import com.camackenzie.exvi.server.util.RequestBodyHandler;

/**
 *
 * @author callum
 */
public class LoginAction
        extends RequestBodyHandler<LoginRequest, AccountAccessKeyResult> {

    public LoginAction() {
        super(LoginRequest.class);
    }

    @Override
    public AccountAccessKeyResult handleBodyRequest(LoginRequest in, Context context) {

        AWSDynamoDB database = new AWSDynamoDB();
        Table userTable = database.cacheTable("exvi-user-login");
        Item userItem = userTable.getItem("username", in.getUsername());

        // Ensure user can be logged in
        if (userItem == null) {
            return new AccountAccessKeyResult(1, "Invalid credentials");
        } else if (!userItem.hasAttribute("passwordHash")) {
            return new AccountAccessKeyResult(1, "Invalid credentials");
        }

        // Retreive user data
        String passwordHashDecrypted = AuthUtils.decryptPasswordHash(in.getPasswordHash());
        String databasePasswordHash = userItem.getString("passwordHash");

        // Check if input password matches database
        if (!databasePasswordHash.equals(passwordHashDecrypted)) {
            return new AccountAccessKeyResult(1, "Invalid credentials");
        }

        // Generate & store access key
        String accessKey = AuthUtils.generateAccessKey(database, in.getUsername());

        // Return access key
        return new AccountAccessKeyResult("Success", accessKey);
    }
}
