/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.lambdas;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.camackenzie.exvi.server.util.*;
import com.camackenzie.exvi.core.api.AccountSaltResult;
import com.camackenzie.exvi.core.api.RetrieveSaltRequest;
import com.camackenzie.exvi.core.util.CryptographyUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public class RetrieveSaltAction
        extends RequestBodyHandler<RetrieveSaltRequest, AccountSaltResult> {

    public RetrieveSaltAction() {
        super(RetrieveSaltRequest.Companion.serializer(), AccountSaltResult.Companion.serializer());
    }

    @Override
    @NotNull
    protected AccountSaltResult handleBodyRequest(@NotNull RetrieveSaltRequest in) {
        // Preconditions
        if (in.getUsername().get().isBlank()) {
            throw new ApiException(400, "No username provided");
        }

        // Retrieve resources
        DocumentDatabase database = getResourceManager().getDatabase();
        Table accountTable = database.getTable("exvi-user-login");
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
