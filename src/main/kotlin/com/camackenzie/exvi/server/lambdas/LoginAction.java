/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.lambdas;

import com.amazonaws.services.dynamodbv2.document.Table;
import com.camackenzie.exvi.core.api.AccountAccessKeyResult;
import com.camackenzie.exvi.core.api.LoginRequest;
import com.camackenzie.exvi.server.database.UserLoginEntry;
import com.camackenzie.exvi.server.util.*;
import org.jetbrains.annotations.NotNull;

// Action to log in an existing user
public class LoginAction implements LambdaAction<LoginRequest, AccountAccessKeyResult> {
    @Override
    public AccountAccessKeyResult enact(@NotNull RequestBodyHandler context, @NotNull LoginRequest in) {
        // Preconditions
        if (in.getUsername().get().isBlank()) throw new ApiException(400, "No username provided");
        if (in.getPasswordHash().get().isBlank()) throw new ApiException(400, "No password provided");

        // Retrieve resources
        DocumentDatabase database = context.getResourceManager().getDatabase();
        Table userTable = database.getTable("exvi-user-login");
        UserLoginEntry entry = database.getObjectFromTable("exvi-user-login", "username",
                in.getUsername().get(), UserLoginEntry.serializer);

        // Ensure user can be logged in
        if (entry == null) throw new ApiException(400, "User does not exist");

        // Retrieve user data
        String passwordHashDecrypted = AuthUtils.decryptPasswordHash(in.getPasswordHash().get());
        String databasePasswordHash = entry.getPasswordHash();

        // Check if input password matches database
        if (!databasePasswordHash.equals(passwordHashDecrypted)) throw new ApiException(400, "Incorrect password");

        // Retrieve and return access key
        String accessKey = AuthUtils.getAccessKey(database, in.getUsername().get());
        return new AccountAccessKeyResult(accessKey);
    }
}
