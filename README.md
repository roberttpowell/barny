# Bacaro Grats Lambda

A serverless version of the Bacaro Grats spreadsheet processing application, converted from Java/Tomcat to AWS Lambda with API Gateway.

## Architecture

- **AWS Lambda**: Java 11 runtime processing spreadsheet uploads
- **API Gateway**: HTTP API serving the web interface and handling file uploads
- **S3**: Static asset storage (optional)
- **CloudWatch**: Logging and monitoring

## Prerequisites

Before deploying, ensure you have the following installed:

1. **Java 11+** - For building the application
2. **Maven 3.6+** - For dependency management and building
3. **AWS CLI** - For AWS authentication and deployment
4. **Terraform 1.0+** - For infrastructure provisioning

### AWS Setup

1. Configure AWS CLI with your credentials:
   ```bash
   aws configure
   ```

2. Ensure your AWS account has appropriate permissions for:
    - Lambda functions
    - API Gateway
    - IAM roles and policies
    - CloudWatch logs
    - S3 buckets (optional)

## Project Structure

```
├── src/main/java/
│   └── com/tiggit/bacaro/lambda/
│       ├── GratsLambdaHandler.java      # Main Lambda handler
│       └── MultipartParser.java          # Multipart form data parser
├── terraform/
│   └── main.tf                          # Infrastructure as code
├── pom.xml                              # Maven dependencies
├── deploy.sh                            # Deployment script
└── README.md                            # This file
```

## Quick Start

### Option 1: One-Command Deployment

Make the deployment script executable and run it:

```bash
chmod +x deploy.sh
./deploy.sh deploy
```

This will:
1. Build the Java application
2. Initialize Terraform
3. Create a deployment plan
4. Deploy all AWS infrastructure
5. Display the application URL

### Option 2: Manual Step-by-Step Deployment

1. **Build the application:**
   ```bash
   mvn clean package -DskipTests
   ```

2. **Initialize Terraform:**
   ```bash
   cd terraform
   terraform init
   cd ..
   ```

3. **Plan the deployment:**
   ```bash
   cd terraform
   terraform plan
   cd ..
   ```

4. **Apply the deployment:**
   ```bash
   cd terraform
   terraform apply
   cd ..
   ```

5. **Get the application URL:**
   ```bash
   cd terraform
   terraform output api_url
   cd ..
   ```

## Usage

After deployment, you'll receive an API Gateway URL. Visit this URL in your browser to access the application.

### Features

- **Drag and Drop**: Drag spreadsheet files directly onto the drop zone
- **Click to Upload**: Click the drop zone to select files via file picker
- **Real-time Processing**: Files are processed immediately upon upload
- **Progress Indicators**: Visual feedback during file processing
- **Error Handling**: Clear error messages for failed uploads

### Supported File Types

- Excel files (.xlsx, .xls)
- CSV files (.csv)
- Other formats supported by your `SpreadSheetReader` implementation

## Updating Code

For quick code updates without infrastructure changes:

```bash
./deploy.sh update
```

This will:
1. Build the updated application
2. Update only the Lambda function code
3. Skip infrastructure changes

## Monitoring

### CloudWatch Logs

View application logs:
1. Go to AWS CloudWatch Console
2. Navigate to Log Groups
3. Find `/aws/lambda/bacaro-grats-processor`

### API Gateway Logs

View request logs:
1. Go to AWS CloudWatch Console
2. Navigate to Log Groups
3. Find `/aws/apigateway/bacaro-grats-api`

## Troubleshooting

### Common Issues

1. **Build Failures**
    - Ensure Java 11+ is installed
    - Check Maven dependencies in `pom.xml`
    - Verify network connectivity for dependency downloads

2. **Deployment Failures**
    - Check AWS credentials: `aws sts get-caller-identity`
    - Verify IAM permissions for Lambda, API Gateway, and CloudWatch
    - Ensure region availability for all services

3. **File Upload Issues**
    - Check file size limits (Lambda has a 6MB request limit)
    - Verify file format compatibility
    - Review CloudWatch logs for detailed error messages

4. **Template Processing Errors**
    - Ensure `templates/payments.vm` is in `src/main/resources/`
    - Check Velocity template syntax
    - Verify `StaffPayment` class structure matches template expectations

### Debugging

Enable verbose logging by setting environment variables in the Terraform configuration:

```hcl
environment {
  variables = {
    ENVIRONMENT = "development"
    LOG_LEVEL   = "DEBUG"
  }
}
```

## Customization

### Modifying the UI

The HTML interface is embedded in the Lambda handler. To modify:

1. Edit the `getIndexHtml()` method in `GratsLambdaHandler.java`
2. Update CSS styles, JavaScript functionality, or HTML structure
3. Redeploy using `./deploy.sh update`

### Adding Static Assets

To serve static files (CSS, images, etc.) from S3:

1. Upload files to the created S3 bucket
2. Update HTML to reference S3 URLs
3. Modify CORS settings if needed

### Scaling Considerations

For high-traffic scenarios:

1. **Increase Lambda memory/timeout** in Terraform configuration
2. **Add CloudFront distribution** for better performance
3. **Implement request throttling** in API Gateway
4. **Use S3 for large file processing** instead of direct Lambda uploads

## Cost Optimization

- **Lambda**: Pay per request and execution time
- **API Gateway**: Pay per API call
- **CloudWatch**: Free tier covers basic logging
- **S3**: Minimal storage costs for static assets

Typical costs for low-moderate usage: $1-5/month

## Security

### Current Security Features

- CORS enabled for web browser access
- IAM roles with minimal required permissions
- CloudWatch logging for audit trails

### Additional Security (Recommended for Production)

1. **Authentication**: Add Cognito or API keys
2. **Input Validation**: Enhanced file type and size validation
3. **Rate Limiting**: API Gateway throttling
4. **VPC**: Deploy Lambda in private VPC if processing sensitive data

## Cleanup

To remove all AWS resources:

```bash
./deploy.sh destroy
```

**Warning**: This will permanently delete all infrastructure and data.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes and test locally
4. Update documentation if needed
5. Submit a pull request

## License

[Add your license information here]

## Support

For issues and questions:
1. Check CloudWatch logs for detailed error information
2. Review this README for common solutions
3. [Add your support contact information]