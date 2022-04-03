/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.lambdas;

import com.camackenzie.exvi.server.util.*;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.camackenzie.exvi.core.api.AccountAccessKeyResult;
import com.camackenzie.exvi.core.util.CryptographyUtils;
import com.camackenzie.exvi.server.database.DatabaseEntry;
import com.camackenzie.exvi.server.database.UserLoginEntry;
import com.camackenzie.exvi.server.database.VerificationDatabaseEntry;
import com.camackenzie.exvi.core.api.AccountCreationRequest;
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.util.ApiException;
import org.jetbrains.annotations.NotNull;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public class SignUpAction extends RequestBodyHandler<AccountCreationRequest, AccountAccessKeyResult> {

    private static final long VERIFICATION_CODE_EXPIRY = 60 * 60 * 1000;

    public SignUpAction() {
        super(AccountCreationRequest.class);
    }

    @Override
    @NotNull
    public AccountAccessKeyResult handleBodyRequest(@NotNull AccountCreationRequest in, @NotNull Context context) {
        // Preconditions
        if (in.getUsername().get().isBlank()) {
            throw new ApiException(400, "No username provided");
        }
        if (in.getPassword().get().isBlank()) {
            throw new ApiException(400, "No password provided");
        }
        if (in.getVerificationCode().get().isBlank()) {
            throw new ApiException(400, "No verification code provided");
        }

        // Retrieve resources
        DocumentDatabase database = new AWSDynamoDB();
        Table userTable = database.getTable("exvi-user-login");

        VerificationDatabaseEntry dbEntry = DatabaseEntry.fromItem(
                userTable.getItem("username", in.getUsername().get()),
                VerificationDatabaseEntry.class);

        if (dbEntry != null) {
            if (this.verificationCodeValid(in.getVerificationCode().get(),
                    dbEntry.getVerificationCode(),
                    dbEntry.getVerificationCodeUTC()
            )) {
                // Verification code is correct
                this.registerAccountData(database, in, dbEntry);
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

    private void registerAccountData(@NotNull DocumentDatabase database,
                                     @NotNull AccountCreationRequest ac,
                                     @NotNull VerificationDatabaseEntry entry) {
        Table userTable = database.getTable("exvi-user-login");
        String salt = CryptographyUtils.generateSalt(32),
                passwordHash = CryptographyUtils.hashSHA256(salt
                        + AuthUtils.decryptPasswordHash(ac.getPassword().get()));
        userTable.deleteItem("username", entry.getUsername());
        database.putObjectInTable(userTable, new UserLoginEntry(ac.getUsername().get(),
                entry.getPhone(),
                entry.getEmail(),
                passwordHash,
                salt
        ));
        UserDataEntry.ensureUserHasData(database, getLogger(), ac.getUsername().get());
    }

    private boolean verificationCodeValid(@NotNull String inputCode, @NotNull String actualCode, long creation) {
        return System.currentTimeMillis() - creation < VERIFICATION_CODE_EXPIRY
                && inputCode.equals(actualCode);
    }

}
