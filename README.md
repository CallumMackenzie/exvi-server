# Exvi Server

The server portion of my high school capstone project.

## Technologies
- Amazon Web Services
  - Lambda & lambda layers for common dependencies
  - SES & SNS for email & text notifications
  - REST API lambda access with API Gateway
  - IAM configured to allocate permissions accordingly
  - Cloudwatch used to monitor requests
  - DynamoDB to store user data
    - Global secondary indices to query used emails & phone numbers
- GitHub Actions for CD
  - Used to deploy the latest server version
  - Custom GitHub actions to access AWS APIs
    - https://github.com/CallumMackenzie/latest-lambda-layer-action
    - https://github.com/CallumMackenzie/trim-lambda-layers-action
- Kotlin/Java
  - Kotlin multiplatform dependencies & interoperability
- Maven
- JitPack

## Related Repositories
- https://github.com/CallumMackenzie/exvi-client
- https://github.com/CallumMackenzie/exvi-core
