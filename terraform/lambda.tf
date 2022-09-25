resource "aws_cloudwatch_log_group" "function_log_group" {
  name              = "/aws/lambda/${aws_lambda_function.s3_upload_trigger_function.function_name}"
  retention_in_days = 7
  lifecycle {
    prevent_destroy = false
  }
}

resource "aws_lambda_function" "s3_upload_trigger_function" {
  function_name    = var.lambda_function_name
  filename         = var.lambda_payload_filename
  role             = aws_iam_role.lambda_iam.arn
  handler          = var.lambda_function_handler
  source_code_hash = "sha256(file(${var.lambda_payload_filename}))"
  runtime          = var.lambda_runtime
  memory_size      = 1596
  timeout          = 60
  environment {
    variables = {
      region = var.region
    }
  }
}

resource "aws_lambda_permission" "s3_upload_trigger_function" {
  statement_id  = "S3UploadTriggerFunction"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.s3_upload_trigger_function.function_name
  principal     = "s3.amazonaws.com"
}

resource "aws_s3_bucket_notification" "s3_upload_trigger" {
  bucket = var.bucket

  lambda_function {
    lambda_function_arn = aws_lambda_function.s3_upload_trigger_function.arn
    events              = ["s3:ObjectCreated:*"]
    filter_suffix = ".pdf"
  }
}

resource "aws_lambda_permission" "s3_upload_trigger_invoke_permission" {
  statement_id  = "AllowS3Invoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.s3_upload_trigger_function.function_name
  principal     = "s3.amazonaws.com"
  source_arn    = "arn:aws:s3:::${var.bucket}"
}
