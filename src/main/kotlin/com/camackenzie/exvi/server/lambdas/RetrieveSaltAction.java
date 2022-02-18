/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.lambdas;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import com.camackenzie.exvi.core.api.AccountSaltResult;
import com.camackenzie.exvi.core.api.RetrieveSaltRequest;
import com.camackenzie.exvi.core.util.CryptographyUtils;
import com.camackenzie.exvi.server.util.AWSDynamoDB;
import com.camackenzie.exvi.server.util.ApiException;

import java.nio.charset.StandardCharsets;

/**
 * @author callum
 */
public class RetrieveSaltAction
        extends RequestBodyHandler<RetrieveSaltRequest, AccountSaltResult> {

    public RetrieveSaltAction() {
        super(RetrieveSaltRequest.class);
    }

    @Override
    public AccountSaltResult handleBodyRequest(RetrieveSaltRequest in, Context context) {
        AWSDynamoDB database = new AWSDynamoDB();
        Table accountTable = database.cacheTable("exvi-user-login");
        Item item = accountTable.getItem("username", in.getUsername().get());

        if (item == null) {
            throw new ApiException(400, "User not found");
        } else if (!item.hasAttribute("salt")) {
            throw new ApiException(400, "No valid user login entry");
        } else {
            return new AccountSaltResult(CryptographyUtils.bytesToBase64String(item.getString("salt")
                            .getBytes(StandardCharsets.UTF_8)));
        }
    }
}
