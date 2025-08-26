#!/bin/bash

# aws-deploy.sh - Deploy using AWS CLI instead of Terraform

set -e

# Configuration
PROJECT_NAME="bacaro-grats"
REGION="us-east-1"
LAMBDA_FUNCTION_NAME="${PROJECT_NAME}-processor"
API_NAME="${PROJECT_NAME}-api"
JAR_FILE="target/bacaro-grats-lambda-1.0.0.jar"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check dependencies
check_dependencies() {
    print_status "Checking dependencies..."

    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed. Please install Maven first."
        exit 1
    fi

    if ! command -v aws &> /dev/null; then
        print_error "AWS CLI is not installed. Please install AWS CLI first."
        exit 1
    fi

    # Test AWS authentication
    if ! aws sts get-caller-identity >/dev/null 2>&1; then
        print_error "AWS CLI is not configured. Please run 'aws configure' first."
        exit 1
    fi

    print_status "All dependencies are installed and configured."
}

# Build the application
build_application() {
    print_status "Building Java application..."

    mvn clean package -DskipTests

    if [ ! -f "$JAR_FILE" ]; then
        print_error "Build failed - JAR file not found: $JAR_FILE"
        exit 1
    fi

    print_status "Application built successfully."
}

# Create IAM role for Lambda
create_iam_role() {
    print_status "Creating IAM role..."

    # Create trust policy
    cat > /tmp/trust-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

    # Create role (ignore if already exists)
    aws iam create-role \
        --role-name "${PROJECT_NAME}-lambda-role" \
        --assume-role-policy-document file:///tmp/trust-policy.json \
        --region $REGION 2>/dev/null || print_status "Role already exists, continuing..."

    # Attach basic execution policy
    aws iam attach-role-policy \
        --role-name "${PROJECT_NAME}-lambda-role" \
        --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole \
        --region $REGION 2>/dev/null || print_status "Policy already attached, continuing..."

    # Get role ARN
    ROLE_ARN=$(aws iam get-role --role-name "${PROJECT_NAME}-lambda-role" --query 'Role.Arn' --output text --region $REGION)
    print_status "IAM Role ARN: $ROLE_ARN"

    # Clean up temp file
    rm -f /tmp/trust-policy.json

    # Wait a moment for IAM role to propagate
    print_status "Waiting for IAM role to propagate..."
    sleep 10
}

# Create or update Lambda function
deploy_lambda() {
    print_status "Deploying Lambda function..."

    # Check if function exists
    if aws lambda get-function --function-name $LAMBDA_FUNCTION_NAME --region $REGION >/dev/null 2>&1; then
        print_status "Updating existing Lambda function..."
        aws lambda update-function-code \
            --function-name $LAMBDA_FUNCTION_NAME \
            --zip-file fileb://$JAR_FILE \
            --region $REGION

        # Update function configuration
        aws lambda update-function-configuration \
            --function-name $LAMBDA_FUNCTION_NAME \
            --handler com.tiggit.bacaro.CommonsFileUploadHandler::handleRequest \
            --timeout 300 \
            --memory-size 1024 \
            --region $REGION
    else
        print_status "Creating new Lambda function..."
        aws lambda create-function \
            --function-name $LAMBDA_FUNCTION_NAME \
            --runtime java11 \
            --role $ROLE_ARN \
            --handler com.tiggit.bacaro.CommonsFileUploadHandler::handleRequest \
            --zip-file fileb://$JAR_FILE \
            --timeout 300 \
            --memory-size 1024 \
            --region $REGION
    fi

    # Wait for function to be ready
    print_status "Waiting for Lambda function to be ready..."
    aws lambda wait function-updated --function-name $LAMBDA_FUNCTION_NAME --region $REGION

    print_status "Lambda function deployed successfully."
}

# Create API Gateway
create_api_gateway() {
    print_status "Setting up API Gateway..."

    # Check if API already exists
    EXISTING_API_ID=$(aws apigatewayv2 get-apis --region $REGION --query "Items[?Name=='$API_NAME'].ApiId" --output text 2>/dev/null || echo "")

    if [ ! -z "$EXISTING_API_ID" ] && [ "$EXISTING_API_ID" != "None" ]; then
        print_status "API Gateway already exists, using existing API: $EXISTING_API_ID"
        API_ID=$EXISTING_API_ID
    else
        print_status "Creating new API Gateway..."
        # Create API
        API_ID=$(aws apigatewayv2 create-api \
            --name $API_NAME \
            --protocol-type HTTP \
            --cors-configuration AllowOrigins="*",AllowMethods="GET,POST,OPTIONS",AllowHeaders="*" \
            --region $REGION \
            --query 'ApiId' --output text)

        print_status "Created API with ID: $API_ID"
    fi

    # Get Lambda function ARN
    LAMBDA_ARN=$(aws lambda get-function --function-name $LAMBDA_FUNCTION_NAME --query 'Configuration.FunctionArn' --output text --region $REGION)

    # Delete existing integration if it exists
    EXISTING_INTEGRATION=$(aws apigatewayv2 get-integrations --api-id $API_ID --region $REGION --query 'Items[0].IntegrationId' --output text 2>/dev/null || echo "")

    if [ ! -z "$EXISTING_INTEGRATION" ] && [ "$EXISTING_INTEGRATION" != "None" ]; then
        print_status "Deleting existing integration: $EXISTING_INTEGRATION"
        aws apigatewayv2 delete-integration --api-id $API_ID --integration-id $EXISTING_INTEGRATION --region $REGION 2>/dev/null || true
    fi

    # Create NEW integration
    print_status "Creating new integration..."
    INTEGRATION_ID=$(aws apigatewayv2 create-integration \
        --api-id $API_ID \
        --integration-type AWS_PROXY \
        --integration-uri $LAMBDA_ARN \
        --payload-format-version 2.0 \
        --region $REGION \
        --query 'IntegrationId' --output text)

    print_status "Created integration with ID: $INTEGRATION_ID"

    # Delete existing routes
    print_status "Cleaning up existing routes..."
    aws apigatewayv2 get-routes --api-id $API_ID --region $REGION --query 'Items[].RouteId' --output text | tr '\t' '\n' | while read -r route_id; do
        if [ ! -z "$route_id" ] && [ "$route_id" != "None" ]; then
            aws apigatewayv2 delete-route --api-id $API_ID --route-id $route_id --region $REGION 2>/dev/null || true
        fi
    done

    # Create a single catch-all route
    print_status "Creating catch-all route..."
    aws apigatewayv2 create-route \
        --api-id $API_ID \
        --route-key '$default' \
        --target integrations/$INTEGRATION_ID \
        --region $REGION >/dev/null

    # Create or update stage
    aws apigatewayv2 create-stage \
        --api-id $API_ID \
        --stage-name '$default' \
        --auto-deploy \
        --region $REGION 2>/dev/null || \
    aws apigatewayv2 update-stage \
        --api-id $API_ID \
        --stage-name '$default' \
        --auto-deploy \
        --region $REGION >/dev/null

    # Add Lambda permission for API Gateway
    ACCOUNT_ID=$(aws sts get-caller-identity --query 'Account' --output text)
    aws lambda add-permission \
        --function-name $LAMBDA_FUNCTION_NAME \
        --statement-id apigateway-invoke \
        --action lambda:InvokeFunction \
        --principal apigateway.amazonaws.com \
        --source-arn "arn:aws:execute-api:$REGION:$ACCOUNT_ID:$API_ID/*/*" \
        --region $REGION 2>/dev/null || print_status "Permission already exists, continuing..."

    # Get API endpoint
    API_ENDPOINT=$(aws apigatewayv2 get-api --api-id $API_ID --query 'ApiEndpoint' --output text --region $REGION)

    print_status "API Gateway configured successfully!"
    print_status "🚀 Application URL: $API_ENDPOINT"
}

# Update existing deployment
update_lambda_only() {
    print_status "Updating Lambda function code only..."
    build_application

    aws lambda update-function-code \
        --function-name $LAMBDA_FUNCTION_NAME \
        --zip-file fileb://$JAR_FILE \
        --region $REGION

    print_status "Lambda function updated successfully!"
}

# Cleanup function
cleanup() {
    print_warning "This will delete all AWS resources for $PROJECT_NAME"
    read -p "Are you sure you want to continue? (y/N): " -n 1 -r
    echo

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_status "Cleanup cancelled."
        return
    fi

    print_warning "Cleaning up AWS resources..."

    # Get API ID if it exists
    API_ID=$(aws apigatewayv2 get-apis --region $REGION --query "Items[?Name=='$API_NAME'].ApiId" --output text 2>/dev/null || echo "")

    # Delete API Gateway
    if [ ! -z "$API_ID" ] && [ "$API_ID" != "None" ]; then
        print_status "Deleting API Gateway: $API_ID"
        aws apigatewayv2 delete-api --api-id $API_ID --region $REGION 2>/dev/null || true
    fi

    # Delete Lambda function
    print_status "Deleting Lambda function: $LAMBDA_FUNCTION_NAME"
    aws lambda delete-function --function-name $LAMBDA_FUNCTION_NAME --region $REGION 2>/dev/null || true

    # Delete IAM role
    print_status "Deleting IAM role: ${PROJECT_NAME}-lambda-role"
    aws iam detach-role-policy --role-name "${PROJECT_NAME}-lambda-role" --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole --region $REGION 2>/dev/null || true
    aws iam delete-role --role-name "${PROJECT_NAME}-lambda-role" --region $REGION 2>/dev/null || true

    print_status "🧹 Cleanup completed."
}

# Show help
show_help() {
    echo "Usage: $0 [OPTION]"
    echo ""
    echo "Options:"
    echo "  deploy     Full deployment (build + infrastructure)"
    echo "  update     Update Lambda function code only"
    echo "  cleanup    Delete all AWS resources"
    echo "  help       Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 deploy     # Full deployment"
    echo "  $0 update     # Quick code update"
    echo "  $0 cleanup    # Remove everything"
}

# Main deployment function
deploy() {
    check_dependencies
    build_application
    create_iam_role
    deploy_lambda
    create_api_gateway

    print_status "🎉 Deployment completed successfully!"
    print_status "📱 Your application is ready at: $API_ENDPOINT"
    print_status ""
    print_status "Next steps:"
    print_status "1. Visit the URL above to test your application"
    print_status "2. Upload a spreadsheet to test the functionality"
    print_status "3. Use '$0 update' for quick code updates"
    print_status "4. Use '$0 cleanup' to remove all resources when done"
}

# Handle script arguments
case "${1:-deploy}" in
    "deploy")
        deploy
        ;;
    "update")
        check_dependencies
        update_lambda_only
        ;;
    "cleanup")
        check_dependencies
        cleanup
        ;;
    "help")
        show_help
        ;;
    *)
        print_error "Unknown option: $1"
        show_help
        exit 1
        ;;
esac