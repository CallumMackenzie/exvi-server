/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.lambdas;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.camackenzie.exvi.core.api.NoneResult;
import com.camackenzie.exvi.core.api.VerificationRequest;
import com.camackenzie.exvi.server.database.VerificationDatabaseEntry;
import com.camackenzie.exvi.server.util.*;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.sns.model.SnsException;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public class VerificationAction
        extends RequestBodyHandler<VerificationRequest, NoneResult> {

    public VerificationAction() {
        super(VerificationRequest.Companion.serializer(), NoneResult.Companion.serializer());
    }

    @Override
    @NotNull
    public NoneResult handleBodyRequest(@NotNull VerificationRequest in) {
        // Preconditions
        if (in.getUsername().get().isBlank()) {
            throw new ApiException(400, "No username provided");
        }
        if (in.getEmail().get().isBlank()) {
            throw new ApiException(400, "No email provided");
        }
        if (in.getPhone().get().isBlank()) {
            throw new ApiException(400, "No phone number provided");
        }
        if (!in.getPhone().get().startsWith("+1")) {
            throw new ApiException(400, "Only Canadian phone numbers are currently supported. "
                    + "Please add '+1' in front of your number.");
        }

        // Retrieve resources
        DocumentDatabase dynamoDB = getResourceManager().getDatabase();
        Table userTable = dynamoDB.getTable("exvi-user-login");

        // Ensure user credentials are valid
        if (this.hasUsernameErrors(userTable, in)) {
            throw new ApiException(400, "Username is invalid");
        } else if (this.hasEmailErrors(userTable, in)) {
            throw new ApiException(400, "Email is invalid");
        } else if (this.hasPhoneErrors(userTable, in)) {
            throw new ApiException(400, "Phone number is invalid");
        } else {
            // Generate verification code
            String code = this.generateVerificationCode();

            boolean codeSent;
            if (this.trySendSMSMessage(in, code)) {
                codeSent = true;
            } else {
                codeSent = this.trySendEmail(in, code);
            }

            if (codeSent) {
                dynamoDB.putObjectInTable(userTable,
                        new VerificationDatabaseEntry(in, code));
                return new NoneResult();
            } else {
                throw new ApiException(500, "Verification code could not be sent");
            }
        }
    }

    private boolean hasPhoneErrors(@NotNull Table userTable, @NotNull VerificationRequest user) {
        try {
            var itemIter = userTable.getIndex("phone-index")
                    .query("phone", user.getPhone().get()).iterator();
            while (itemIter.hasNext()) {
                Item next = itemIter.next();
                if (next.getString("phone").equalsIgnoreCase(user.getPhone().get())) {
                    String phoneUser = itemIter.next().getString("username");
                    Item userItem = userTable.getItem("username", phoneUser);
                    return !userItem.hasAttribute("verificationCode");
                }
            }
            return false;
        } catch (Exception e) {
            this.getLogger().e("Phone validation error", e, null);
            return true;
        }
    }

    private boolean hasEmailErrors(@NotNull Table userTable, @NotNull VerificationRequest user) {
        try {
            for (Item next : userTable.getIndex("email-index")
                    .query("email", user.getEmail().get())) {
                if (next.getString("email").equalsIgnoreCase(user.getEmail().get())) {
                    String emailUser = next.getString("username");
                    Item userItem = userTable.getItem("username", emailUser);
                    return !userItem.hasAttribute("verificationCode");
                }
            }
            return false;
        } catch (Exception e) {
            this.getLogger().e("Email validation error: ", e, null);
            return true;
        }
    }

    private boolean hasUsernameErrors(@NotNull Table userTable, @NotNull VerificationRequest user) {
        Item outcome = userTable.getItem("username", user.getUsername().get());
        if (outcome != null) {
            return !outcome.hasAttribute("verificationCode");
        } else {
            return false;
        }
    }

    @NotNull
    private String generateVerificationCode() {
        int intCode = (int) (Math.random() * 999999);
        String code = Integer.toString(intCode);
        return "0".repeat(6 - code.length()) + code;
    }

    private boolean trySendEmail(@NotNull VerificationRequest user, @NotNull String code) {
        StringBuilder htmlBody = new StringBuilder()
                .append("<h2>Hello ")
                .append(user.getUsername().get())
                .append("!</h2>")
                .append("<p>Your exvi user verification code is:</p>")
                .append("<h3>")
                .append(code)
                .append("</h3>");
        StringBuilder textBody = new StringBuilder()
                .append("Hello ")
                .append(user.getUsername().get())
                .append("!\n\nYour Exvi user verification code is ")
                .append(code)
                .append(".");
        try {
            EmailClient emailClient = getResourceManager().getEmailClient();
            emailClient.sendEmail("exvi@camackenzie.com",
                    user.getEmail().get(),
                    "Exvi Verification Code",
                    htmlBody.toString(),
                    textBody.toString());
            return true;
        } catch (Exception ex) {
            this.getLogger().e("Verification code email was not sent.", ex, null);
        }
        return false;
    }

    private boolean trySendSMSMessage(@NotNull VerificationRequest user, @NotNull String code) {
        StringBuilder textContent = new StringBuilder()
                .append("Hello ")
                .append(user.getUsername().get())
                .append("! Your verification code for Exvi is ")
                .append(code)
                .append(".");
        try {
            SMSClient smsc = getResourceManager().getSMSClient();
            smsc.sendText(user.getPhone().get(), textContent.toString());
            return true;
        } catch (SnsException e) {
            this.getLogger().e("SNS error", e, null);
        } catch (Exception e) {
            this.getLogger().e("SNS client warning", e, null);
        }
        return false;
    }

}
