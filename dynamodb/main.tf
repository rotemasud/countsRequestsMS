resource "aws_dynamodb_table" "basic-dynamodb-table" {
  name           = var.name
  read_capacity  = 5
  write_capacity = 5
  hash_key       = "Id"
  
  attribute {
    name = "Id"
    type = "S"
  }

  tags = {
    Name        = var.name
    Environment = var.environment
  }
}