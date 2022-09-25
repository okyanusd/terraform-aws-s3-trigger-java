variable "region" {
  default = "eu-central-1"
}

variable "lambda_role_name" {
  default = "s3_upload_trigger_role"
}

variable "lambda_iam_policy_name" {
  default = "s3_upload_trigger_policy"
}
variable "lambda_payload_filename" {
  default = "../lambda/target/lambda-1.0.0.jar"
}

variable "lambda_function_name" {
  default = "s3_upload_trigger"
}

variable "lambda_function_handler" {
  default = "com.mihsap.lambda.S3UploadHandler"
}

variable "lambda_runtime" {
  default = "java11"
}

variable "api_env_stage_name" {
  default = "beta"
}

variable "bucket" {
  default = "okyanusd"
}