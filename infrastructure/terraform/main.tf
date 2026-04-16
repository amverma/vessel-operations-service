terraform {
  required_version = ">= 1.0"
  
  backend "s3" {
    bucket = "vessel-ops-terraform-state"
    key    = "vessel-operations-service/terraform.tfstate"
    region = "us-east-1"
  }
}

provider "aws" {
  region = var.aws_region
}

resource "aws_ecs_cluster" "vessel_ops" {
  name = "vessel-operations-cluster"
  
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_ecs_task_definition" "vessel_ops_service" {
  family                   = "vessel-operations-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  
  container_definitions = jsonencode([{
    name  = "vessel-operations-service"
    image = "vessel-operations-service:latest"
    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]
  }])
}