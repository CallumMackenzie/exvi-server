package com.camackenzie.exvi.server.util;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.camackenzie.exvi.server.test.*;
import org.jetbrains.annotations.NotNull;
import com.amazonaws.services.lambda.runtime.Context;

import java.util.function.Supplier;

public final class AWSResourceManager {

    private final CachedSupplier<DocumentDatabase> databaseSupplier;
    private final CachedSupplier<SMSClient> smsSupplier;
    private final CachedSupplier<EmailClient> emailClientSupplier;
    private final CachedSupplier<LambdaLogger> lambdaLoggerSupplier;
    public final Context context;

    private AWSResourceManager(@NotNull Context context,
                               @NotNull Supplier<DocumentDatabase> databaseSupplier,
                               @NotNull Supplier<SMSClient> smsSupplier,
                               @NotNull Supplier<EmailClient> emailClientSupplier,
                               @NotNull Supplier<LambdaLogger> lambdaLoggerSupplier) {
        this.databaseSupplier = new CachedSupplier<>(databaseSupplier);
        this.smsSupplier = new CachedSupplier<>(smsSupplier);
        this.emailClientSupplier = new CachedSupplier<>(emailClientSupplier);
        this.lambdaLoggerSupplier = new CachedSupplier<>(lambdaLoggerSupplier);
        this.context = context;
    }

    public DocumentDatabase getDatabase() {
        return databaseSupplier.get();
    }

    public SMSClient getSMSClient() {
        return smsSupplier.get();
    }

    public EmailClient getEmailClient() {
        return emailClientSupplier.get();
    }

    public LambdaLogger getLogger() {
        return lambdaLoggerSupplier.get();
    }

    public static AWSResourceManager get(Context context) {
        if (context instanceof TestContext) {
            return new AWSResourceManager(context,
                    () -> new AWSDynamoDB(new TestAWSDynamoDB()),
                    TestSMSClient::new,
                    TestEmailClient::new,
                    TestLambdaLogger::new);
        }
        return new AWSResourceManager(context,
                AWSDynamoDB::new,
                AWSSMSClient::new,
                AWSEmailClient::new,
                context::getLogger);
    }

}
