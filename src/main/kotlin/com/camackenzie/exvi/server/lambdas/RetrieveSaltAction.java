/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.lambdas;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.camackenzie.exvi.core.api.AccountSaltResult;
import com.camackenzie.exvi.core.api.RetrieveSaltRequest;
import com.camackenzie.exvi.core.util.CryptographyUtils;
import com.camackenzie.exvi.server.util.ApiException;
import com.camackenzie.exvi.server.util.DocumentDatabase;
import com.camackenzie.exvi.server.util.LambdaAction;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public class RetrieveSaltAction implements LambdaAction<RetrieveSaltRequest, AccountSaltResult> {
    @Override
    public AccountSaltResult enact(@NotNull RequestBodyHandler context, @NotNull RetrieveSaltRequest in) {
        // Preconditions
        if (in.getUsername().get().isBlank()) throw new ApiException(400, "No username provided");

        // Retrieve resources
        DocumentDatabase database = context.getResourceManager().getDatabase();
        Table accountTable = database.getTable("exvi-user-login");
        Item item = accountTable.getItem("username", in.getUsername().get());

        if (item == null) throw new ApiException(400, "User not found");
        else if (!item.hasAttribute("salt")) throw new ApiException(400, "No valid user login entry");
        else // Encode and return salt
            return new AccountSaltResult(CryptographyUtils.bytesToBase64String(item.getString("salt")
                    .getBytes(StandardCharsets.UTF_8)));
    }
}
