resource "aws_dynamodb_table" "basic-dynamodb-table" {
  name           = var.name
  read_capacity  = 2
  write_capacity = 2
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