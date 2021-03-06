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
import com.camackenzie.exvi.server.database.UserVerificationEntry;
import com.camackenzie.exvi.server.util.*;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.sns.model.SnsException;

/**
 * @author callum
 */
// Action to send verification messages to users for account creation
public class UserVerificationAction implements LambdaAction<VerificationRequest, NoneResult> {

    private static final String LOG_TAG = "VERIFICATION";

    @Override
    public NoneResult enact(@NotNull RequestBodyHandler context, @NotNull VerificationRequest in) {
        // Preconditions
        if (in.getUsername().get().isBlank()) throw new ApiException(400, "No username provided");
        if (in.getEmail().get().isBlank()) throw new ApiException(400, "No email provided");
        if (in.getPhone().get().isBlank()) throw new ApiException(400, "No phone number provided");
        if (!in.getPhone().get().startsWith("+1"))
            throw new ApiException(400, "Only Canadian phone numbers are currently supported. "
                    + "Please add '+1' in front of your number.");

        // Retrieve resources
        DocumentDatabase dynamoDB = context.getResourceManager().getDatabase();
        Table userTable = dynamoDB.getTable("exvi-user-login");

        // Ensure user credentials are valid
        if (hasUsernameErrors(userTable, in)) throw new ApiException(400, "Username is invalid");
        else if (hasEmailErrors(context, userTable, in)) throw new ApiException(400, "Email is invalid");
        else if (hasPhoneErrors(context, userTable, in)) throw new ApiException(400, "Phone number is invalid");
        else {
            // Generate verification code
            String code = generateVerificationCode();

            // Send SMS message
            boolean codeSent;
            if (trySendSMSMessage(context, in, code)) codeSent = true;
            else // Code could not be sent via SMS
                codeSent = trySendEmail(context, in, code);

            if (codeSent) {
                // Create a user verification entry in database for later account creation
                dynamoDB.putObjectInTable(userTable, UserVerificationEntry.serializer,
                        new UserVerificationEntry(in, code));
                return new NoneResult();
            } else // Code could not be sent via SMS or email
                throw new ApiException(500, "Verification code could not be sent");
        }
    }

    private static boolean hasPhoneErrors(@NotNull RequestBodyHandler context,
                                          @NotNull Table userTable,
                                          @NotNull VerificationRequest user) {
        try {
            // Query global secondary index on login entries by phone number
            var itemIter = userTable.getIndex("phone-index")
                    .query("phone", user.getPhone().get()).iterator();
            // For each query response
            while (itemIter.hasNext()) {
                Item next = itemIter.next();
                // Check if phone numbers are equal
                if (next.getString("phone").equalsIgnoreCase(user.getPhone().get())) {
                    // Retrieve user with phone number
                    String phoneUser = itemIter.next().getString("username");
                    Item userItem = userTable.getItem("username", phoneUser);
                    // Return true if user has a verified account already
                    return !userItem.hasAttribute("verificationCode");
                }
            }
            // Phone has no linked account
            return false;
        } catch (Exception e) {
            context.getLogger().e("Phone validation error", e, LOG_TAG);
            return true;
        }
    }

    private static boolean hasEmailErrors(@NotNull RequestBodyHandler context,
                                          @NotNull Table userTable,
                                          @NotNull VerificationRequest user) {
        try {
            // Query global secondary index on login entries by email
            for (Item next : userTable.getIndex("email-index")
                    .query("email", user.getEmail().get())) {
                // Check if emails are equal
                if (next.getString("email").equalsIgnoreCase(user.getEmail().get())) {
                    // Retrieve user with email
                    String emailUser = next.getString("username");
                    Item userItem = userTable.getItem("username", emailUser);
                    // Return true if user has a verified account already
                    return !userItem.hasAttribute("verificationCode");
                }
            }
            // Email has no linked account
            return false;
        } catch (Exception e) {
            context.getLogger().e("Email validation error: ", e, LOG_TAG);
            return true;
        }
    }

    private static boolean hasUsernameErrors(@NotNull Table userTable, @NotNull VerificationRequest req) {
        // Check if user exists
        Item user = userTable.getItem("username", req.getUsername().get());
        if (user != null) // Return true if user has a verified account already
            return !user.hasAttribute("verificationCode");
        else // User has no verified account
            return false;
    }

    @NotNull
    private static String generateVerificationCode() {
        // Get a integer representing a 6-digit verification code
        int intCode = (int) (Math.random() * 999999);
        // Prepend zeros to code if it is not a 6-digit number
        String code = Integer.toString(intCode);
        return "0".repeat(6 - code.length()) + code;
    }

    private static boolean trySendEmail(@NotNull RequestBodyHandler context,
                                        @NotNull VerificationRequest user,
                                        @NotNull String code) {
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
            EmailClient emailClient = context.getResourceManager().getEmailClient();
            emailClient.sendEmail("exvi@camackenzie.com",
                    user.getEmail().get(),
                    "Exvi Verification Code",
                    htmlBody.toString(),
                    textBody.toString());
            return true;
        } catch (Exception ex) {
            context.getLogger().e("Verification code email was not sent.", ex, LOG_TAG);
        }
        return false;
    }

    private static boolean trySendSMSMessage(@NotNull RequestBodyHandler context,
                                             @NotNull VerificationRequest user,
                                             @NotNull String code) {
        StringBuilder textContent = new StringBuilder()
                .append("Hello ")
                .append(user.getUsername().get())
                .append("! Your verification code for Exvi is ")
                .append(code)
                .append(".");
        try {
            SMSClient client = context.getResourceManager().getSMSClient();
            client.sendText(user.getPhone().get(), textContent.toString());
            return true;
        } catch (SnsException e) {
            context.getLogger().e("SNS error", e, LOG_TAG);
        } catch (Exception e) {
            context.getLogger().e("SNS client warning", e, LOG_TAG);
        }
        return false;
    }

}
