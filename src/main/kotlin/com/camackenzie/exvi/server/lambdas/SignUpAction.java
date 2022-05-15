/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.lambdas;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.camackenzie.exvi.core.api.AccountAccessKeyResult;
import com.camackenzie.exvi.core.api.AccountCreationRequest;
import com.camackenzie.exvi.core.model.ExviSerializer;
import com.camackenzie.exvi.core.util.CryptographyUtils;
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.database.UserLoginEntry;
import com.camackenzie.exvi.server.database.UserVerificationEntry;
import com.camackenzie.exvi.server.util.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public class SignUpAction implements LambdaAction<AccountCreationRequest, AccountAccessKeyResult> {

    private static final long VERIFICATION_CODE_EXPIRY = 60 * 60 * 1000;

    @Override
    public AccountAccessKeyResult enact(@NotNull RequestBodyHandler context, @NotNull AccountCreationRequest in) {
        // Preconditions
        if (in.getUsername().get().isBlank()) throw new ApiException(400, "No username provided");
        if (in.getPassword().get().isBlank()) throw new ApiException(400, "No password provided");
        if (in.getVerificationCode().get().isBlank()) throw new ApiException(400, "No verification code provided");

        // Retrieve resources
        DocumentDatabase database = context.getResourceManager().getDatabase();
        Table userTable = database.getTable("exvi-user-login");

        // Check if user has verification data
        Item userItem = userTable.getItem("username", in.getUsername().get());
        boolean hasVerificationData = userItem.get("verificationCodeUTC") != null;

        if (hasVerificationData) {
            var dbEntry = ExviSerializer.fromJson(UserVerificationEntry.serializer, userItem.toJSON());
            if (verificationCodeValid(in.getVerificationCode().get(),
                    dbEntry.getVerificationCode(),
                    dbEntry.getVerificationCodeUTC()
            )) {
                // Verification code is correct
                registerAccountData(database, in, dbEntry);
                String accessKey = AuthUtils.generateAccessKey(database, in.getUsername().get());
                return new AccountAccessKeyResult(accessKey);
            } else {
                // Verification code is incorrect
                throw new ApiException(400, "Verification code is incorrect or expired");
            }
        } else {
            throw new ApiException(400, "User does not have verification data");
        }
    }

    private static void registerAccountData(@NotNull DocumentDatabase database,
                                            @NotNull AccountCreationRequest ac,
                                            @NotNull UserVerificationEntry entry) {
        Table userTable = database.getTable("exvi-user-login");
        String salt = CryptographyUtils.generateSalt(32),
                passwordHash = CryptographyUtils.hashSHA256(salt
                        + AuthUtils.decryptPasswordHash(ac.getPassword().get()));
        userTable.deleteItem("username", entry.getUsername());
        database.putObjectInTable(userTable, UserLoginEntry.serializer, new UserLoginEntry(ac.getUsername().get(),
                entry.getPhone(),
                entry.getEmail(),
                passwordHash,
                salt
        ));
        UserDataEntry.ensureUserHasData(database, ac.getUsername().get());
    }

    private static boolean verificationCodeValid(@NotNull String inputCode, @NotNull String actualCode, long creation) {
        return System.currentTimeMillis() - creation < VERIFICATION_CODE_EXPIRY
                && inputCode.equals(actualCode);
    }

}
