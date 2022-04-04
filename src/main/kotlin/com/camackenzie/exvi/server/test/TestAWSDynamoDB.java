package com.camackenzie.exvi.server.test;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.regions.Region;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.waiters.AmazonDynamoDBWaiters;

import java.util.List;
import java.util.Map;

// TODO: Make this usable for tests?
public class TestAWSDynamoDB implements AmazonDynamoDB {
    private String endpoint = "";
    private Region region;

    @Override
    public void setEndpoint(String s) {
        endpoint = s;
    }

    @Override
    public void setRegion(Region region) {
        this.region = region;
    }

    @Override
    public BatchExecuteStatementResult batchExecuteStatement(BatchExecuteStatementRequest batchExecuteStatementRequest) {
        return null;
    }

    @Override
    public BatchGetItemResult batchGetItem(BatchGetItemRequest batchGetItemRequest) {
        return null;
    }

    @Override
    public BatchGetItemResult batchGetItem(Map<String, KeysAndAttributes> map, String s) {
        return null;
    }

    @Override
    public BatchGetItemResult batchGetItem(Map<String, KeysAndAttributes> map) {
        return null;
    }

    @Override
    public BatchWriteItemResult batchWriteItem(BatchWriteItemRequest batchWriteItemRequest) {
        return null;
    }

    @Override
    public BatchWriteItemResult batchWriteItem(Map<String, List<WriteRequest>> map) {
        return null;
    }

    @Override
    public CreateBackupResult createBackup(CreateBackupRequest createBackupRequest) {
        return null;
    }

    @Override
    public CreateGlobalTableResult createGlobalTable(CreateGlobalTableRequest createGlobalTableRequest) {
        return null;
    }

    @Override
    public CreateTableResult createTable(CreateTableRequest createTableRequest) {
        return null;
    }

    @Override
    public CreateTableResult createTable(List<AttributeDefinition> list, String s, List<KeySchemaElement> list1, ProvisionedThroughput provisionedThroughput) {
        return null;
    }

    @Override
    public DeleteBackupResult deleteBackup(DeleteBackupRequest deleteBackupRequest) {
        return null;
    }

    @Override
    public DeleteItemResult deleteItem(DeleteItemRequest deleteItemRequest) {
        return null;
    }

    @Override
    public DeleteItemResult deleteItem(String s, Map<String, AttributeValue> map) {
        return null;
    }

    @Override
    public DeleteItemResult deleteItem(String s, Map<String, AttributeValue> map, String s1) {
        return null;
    }

    @Override
    public DeleteTableResult deleteTable(DeleteTableRequest deleteTableRequest) {
        return null;
    }

    @Override
    public DeleteTableResult deleteTable(String s) {
        return null;
    }

    @Override
    public DescribeBackupResult describeBackup(DescribeBackupRequest describeBackupRequest) {
        return null;
    }

    @Override
    public DescribeContinuousBackupsResult describeContinuousBackups(DescribeContinuousBackupsRequest describeContinuousBackupsRequest) {
        return null;
    }

    @Override
    public DescribeContributorInsightsResult describeContributorInsights(DescribeContributorInsightsRequest describeContributorInsightsRequest) {
        return null;
    }

    @Override
    public DescribeEndpointsResult describeEndpoints(DescribeEndpointsRequest describeEndpointsRequest) {
        return null;
    }

    @Override
    public DescribeExportResult describeExport(DescribeExportRequest describeExportRequest) {
        return null;
    }

    @Override
    public DescribeGlobalTableResult describeGlobalTable(DescribeGlobalTableRequest describeGlobalTableRequest) {
        return null;
    }

    @Override
    public DescribeGlobalTableSettingsResult describeGlobalTableSettings(DescribeGlobalTableSettingsRequest describeGlobalTableSettingsRequest) {
        return null;
    }

    @Override
    public DescribeKinesisStreamingDestinationResult describeKinesisStreamingDestination(DescribeKinesisStreamingDestinationRequest describeKinesisStreamingDestinationRequest) {
        return null;
    }

    @Override
    public DescribeLimitsResult describeLimits(DescribeLimitsRequest describeLimitsRequest) {
        return null;
    }

    @Override
    public DescribeTableResult describeTable(DescribeTableRequest describeTableRequest) {
        return null;
    }

    @Override
    public DescribeTableResult describeTable(String s) {
        return null;
    }

    @Override
    public DescribeTableReplicaAutoScalingResult describeTableReplicaAutoScaling(DescribeTableReplicaAutoScalingRequest describeTableReplicaAutoScalingRequest) {
        return null;
    }

    @Override
    public DescribeTimeToLiveResult describeTimeToLive(DescribeTimeToLiveRequest describeTimeToLiveRequest) {
        return null;
    }

    @Override
    public DisableKinesisStreamingDestinationResult disableKinesisStreamingDestination(DisableKinesisStreamingDestinationRequest disableKinesisStreamingDestinationRequest) {
        return null;
    }

    @Override
    public EnableKinesisStreamingDestinationResult enableKinesisStreamingDestination(EnableKinesisStreamingDestinationRequest enableKinesisStreamingDestinationRequest) {
        return null;
    }

    @Override
    public ExecuteStatementResult executeStatement(ExecuteStatementRequest executeStatementRequest) {
        return null;
    }

    @Override
    public ExecuteTransactionResult executeTransaction(ExecuteTransactionRequest executeTransactionRequest) {
        return null;
    }

    @Override
    public ExportTableToPointInTimeResult exportTableToPointInTime(ExportTableToPointInTimeRequest exportTableToPointInTimeRequest) {
        return null;
    }

    @Override
    public GetItemResult getItem(GetItemRequest getItemRequest) {
        return null;
    }

    @Override
    public GetItemResult getItem(String s, Map<String, AttributeValue> map) {
        return null;
    }

    @Override
    public GetItemResult getItem(String s, Map<String, AttributeValue> map, Boolean aBoolean) {
        return null;
    }

    @Override
    public ListBackupsResult listBackups(ListBackupsRequest listBackupsRequest) {
        return null;
    }

    @Override
    public ListContributorInsightsResult listContributorInsights(ListContributorInsightsRequest listContributorInsightsRequest) {
        return null;
    }

    @Override
    public ListExportsResult listExports(ListExportsRequest listExportsRequest) {
        return null;
    }

    @Override
    public ListGlobalTablesResult listGlobalTables(ListGlobalTablesRequest listGlobalTablesRequest) {
        return null;
    }

    @Override
    public ListTablesResult listTables(ListTablesRequest listTablesRequest) {
        return null;
    }

    @Override
    public ListTablesResult listTables() {
        return null;
    }

    @Override
    public ListTablesResult listTables(String s) {
        return null;
    }

    @Override
    public ListTablesResult listTables(String s, Integer integer) {
        return null;
    }

    @Override
    public ListTablesResult listTables(Integer integer) {
        return null;
    }

    @Override
    public ListTagsOfResourceResult listTagsOfResource(ListTagsOfResourceRequest listTagsOfResourceRequest) {
        return null;
    }

    @Override
    public PutItemResult putItem(PutItemRequest putItemRequest) {
        return null;
    }

    @Override
    public PutItemResult putItem(String s, Map<String, AttributeValue> map) {
        return null;
    }

    @Override
    public PutItemResult putItem(String s, Map<String, AttributeValue> map, String s1) {
        return null;
    }

    @Override
    public QueryResult query(QueryRequest queryRequest) {
        return null;
    }

    @Override
    public RestoreTableFromBackupResult restoreTableFromBackup(RestoreTableFromBackupRequest restoreTableFromBackupRequest) {
        return null;
    }

    @Override
    public RestoreTableToPointInTimeResult restoreTableToPointInTime(RestoreTableToPointInTimeRequest restoreTableToPointInTimeRequest) {
        return null;
    }

    @Override
    public ScanResult scan(ScanRequest scanRequest) {
        return null;
    }

    @Override
    public ScanResult scan(String s, List<String> list) {
        return null;
    }

    @Override
    public ScanResult scan(String s, Map<String, Condition> map) {
        return null;
    }

    @Override
    public ScanResult scan(String s, List<String> list, Map<String, Condition> map) {
        return null;
    }

    @Override
    public TagResourceResult tagResource(TagResourceRequest tagResourceRequest) {
        return null;
    }

    @Override
    public TransactGetItemsResult transactGetItems(TransactGetItemsRequest transactGetItemsRequest) {
        return null;
    }

    @Override
    public TransactWriteItemsResult transactWriteItems(TransactWriteItemsRequest transactWriteItemsRequest) {
        return null;
    }

    @Override
    public UntagResourceResult untagResource(UntagResourceRequest untagResourceRequest) {
        return null;
    }

    @Override
    public UpdateContinuousBackupsResult updateContinuousBackups(UpdateContinuousBackupsRequest updateContinuousBackupsRequest) {
        return null;
    }

    @Override
    public UpdateContributorInsightsResult updateContributorInsights(UpdateContributorInsightsRequest updateContributorInsightsRequest) {
        return null;
    }

    @Override
    public UpdateGlobalTableResult updateGlobalTable(UpdateGlobalTableRequest updateGlobalTableRequest) {
        return null;
    }

    @Override
    public UpdateGlobalTableSettingsResult updateGlobalTableSettings(UpdateGlobalTableSettingsRequest updateGlobalTableSettingsRequest) {
        return null;
    }

    @Override
    public UpdateItemResult updateItem(UpdateItemRequest updateItemRequest) {
        return null;
    }

    @Override
    public UpdateItemResult updateItem(String s, Map<String, AttributeValue> map, Map<String, AttributeValueUpdate> map1) {
        return null;
    }

    @Override
    public UpdateItemResult updateItem(String s, Map<String, AttributeValue> map, Map<String, AttributeValueUpdate> map1, String s1) {
        return null;
    }

    @Override
    public UpdateTableResult updateTable(UpdateTableRequest updateTableRequest) {
        return null;
    }

    @Override
    public UpdateTableResult updateTable(String s, ProvisionedThroughput provisionedThroughput) {
        return null;
    }

    @Override
    public UpdateTableReplicaAutoScalingResult updateTableReplicaAutoScaling(UpdateTableReplicaAutoScalingRequest updateTableReplicaAutoScalingRequest) {
        return null;
    }

    @Override
    public UpdateTimeToLiveResult updateTimeToLive(UpdateTimeToLiveRequest updateTimeToLiveRequest) {
        return null;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest amazonWebServiceRequest) {
        return null;
    }

    @Override
    public AmazonDynamoDBWaiters waiters() {
        return null;
    }
}
