/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.lambdas;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.camackenzie.exvi.core.api.VerificationRequest;
import com.camackenzie.exvi.core.api.VerificationResult;
import com.camackenzie.exvi.server.database.VerificationDatabaseEntry;
import com.camackenzie.exvi.server.util.AWSDynamoDB;
import com.camackenzie.exvi.server.util.AWSEmailClient;
import com.camackenzie.exvi.server.util.AWSSMSClient;
import com.camackenzie.exvi.server.util.EmailClient;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import com.camackenzie.exvi.server.util.SMSClient;
import software.amazon.awssdk.services.sns.model.SnsException;

/**
 *
 * @author callum
 */
public class VerificationAction
        extends RequestBodyHandler<VerificationRequest, VerificationResult> {

    public VerificationAction() {
        super(VerificationRequest.class);
    }

    @Override
    public VerificationResult handleBodyRequest(VerificationRequest in, Context context) {
        AWSDynamoDB dynamoDB = new AWSDynamoDB();
        Table userTable = dynamoDB.getTable("exvi-user-login");

        // Ensure user credentials are valid
        if (this.hasUsernameErrors(userTable, in)) {
            return new VerificationResult(1, "Username is invalid");
        } else if (this.hasEmailErrors(userTable, in)) {
            return new VerificationResult(2, "Email is invalid");
        } else if (this.hasPhoneErrors(userTable, in)) {
            return new VerificationResult(3, "Phone number is invalid");
        } else {
            // Generate verification code
            String code = this.generateVerificationCode();

            boolean codeSent = false;
            if (this.trySendSMSMessage(in, code)) {
                codeSent = true;
            } else {
                codeSent = this.trySendEmail(in, code);
            }

            if (codeSent) {
                dynamoDB.putObjectInTable(userTable,
                        new VerificationDatabaseEntry(in, code));
                return new VerificationResult(0, "Verification code sent");
            } else {
                return new VerificationResult(4, "Verification code could not be sent");
            }
        }
    }

    private boolean hasPhoneErrors(Table userTable, VerificationRequest user) {
        if (user.getPhone() == null
                || user.getPhone().isBlank()) {
            return true;
        }
        try {
            var itemIter = userTable.getIndex("phone-index").query("phone", user.getPhone()).iterator();
            if (itemIter.hasNext()) {
                String phoneUser = itemIter.next().getString("username");
                Item userItem = userTable.getItem("username", phoneUser);
                return !userItem.hasAttribute("verificationCode");
            } else {
                return false;
            }
        } catch (Exception e) {
            this.getLogger().log("Phone validation error: " + e);
        } finally {
            return true;
        }
    }

    private boolean hasEmailErrors(Table userTable, VerificationRequest user) {
        if (user.getEmail() == null
                || user.getEmail().isBlank()) {
            return true;
        }
        try {
            var itemIter = userTable.getIndex("email-index").query("email", user.getEmail()).iterator();
            if (itemIter.hasNext()) {
                String emailUser = itemIter.next().getString("username");
                Item userItem = userTable.getItem("username", emailUser);
                return !userItem.hasAttribute("verificationCode");
            } else {
                return false;
            }
        } catch (Exception e) {
            this.getLogger().log("Email validation error: " + e);
        } finally {
            return true;
        }
    }

    private boolean hasUsernameErrors(Table userTable, VerificationRequest user) {
        if (user.getUsername() == null
                || user.getUsername().isBlank()) {
            return true;
        }
        Item outcome = userTable.getItem("username", user.getUsername());
        if (outcome != null) {
            return !outcome.hasAttribute("verificationCode");
        } else {
            return false;
        }
    }

    private String generateVerificationCode() {
        int intCode = (int) (Math.random() * 999999);
        String code = Integer.toString(intCode);
        return "0".repeat(6 - code.length()) + code;
    }

    private boolean trySendEmail(VerificationRequest user, String code) {
        if (user.getEmail().isBlank()) {
            return false;
        }
        StringBuilder htmlBody = new StringBuilder()
                .append("<h2>Hello ")
                .append(user.getUsername())
                .append("!</h2>")
                .append("<p>Your exvi user verification code is:</p>")
                .append("<h3>")
                .append(code)
                .append("</h3>");
        StringBuilder textBody = new StringBuilder()
                .append("Hello ")
                .append(user.getUsername())
                .append("!\n\nYour Exvi user verification code is ")
                .append(code)
                .append(".");
        try {
            EmailClient emailClient = new AWSEmailClient();
            emailClient.sendEmail("exvi@camackenzie.com",
                    user.getEmail(),
                    "Exvi Verification Code",
                    htmlBody.toString(),
                    textBody.toString());
            return true;
        } catch (Exception ex) {
            this.getLogger().log("Verification code email was not sent. Error message: "
                    + ex.getMessage());
        }
        return false;
    }

    private boolean trySendSMSMessage(VerificationRequest user, String code) {
        if (user.getPhone().isBlank()) {
            return false;
        }
        StringBuilder textContent = new StringBuilder()
                .append("Hello ")
                .append(user.getUsername())
                .append("! Your verification code for Exvi is ")
                .append(code)
                .append(".");
        try {
            SMSClient smsc = new AWSSMSClient();
            smsc.sendText(user.getPhone(), textContent.toString());
            return true;
        } catch (SnsException e) {
            this.getLogger().log("SNS WARNING: " + e.awsErrorDetails().errorMessage() + "\n");
        } catch (Exception e) {
            this.getLogger().log("SNS CLIENT WARNING: " + e.toString());
        }
        return false;
    }

}
