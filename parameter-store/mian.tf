resource "aws_ssm_parameter" "store" {
  name  = var.name
  type  = "String"
  value = var.value
}