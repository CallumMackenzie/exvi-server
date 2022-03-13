/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import org.jetbrains.annotations.NotNull;

/**
 * @author callum
 */
public class AWSEmailClient implements EmailClient {

    @NotNull
    private final AmazonSimpleEmailService ases;

    public AWSEmailClient() {
        this.ases = AmazonSimpleEmailServiceClientBuilder.standard()
                .withRegion(Regions.US_EAST_2).build();
    }

    @Override
    public void sendEmail(@NotNull String sender,
                          @NotNull String recipient,
                          @NotNull String subject,
                          @NotNull String htmlBody,
                          @NotNull String plainTextBody) {
        SendEmailRequest ser = new SendEmailRequest()
                .withDestination(new Destination().withToAddresses(recipient))
                .withMessage(new Message()
                        .withBody(new Body()
                                .withHtml(new Content()
                                        .withCharset("UTF-8")
                                        .withData(htmlBody))
                                .withText(new Content()
                                        .withCharset("UTF-8")
                                        .withData(plainTextBody)))
                        .withSubject(new Content()
                                .withCharset("UTF-8")
                                .withData(subject)))
                .withSource(sender);
        this.ases.sendEmail(ser);
    }

}
