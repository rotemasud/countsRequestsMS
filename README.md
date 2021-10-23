# countsRequestsMS

micro service Url: http://demo-alb-prod-1095809523.eu-central-1.elb.amazonaws.com/count

- spring boot micro service which counts the number of requests 
- the spring boot is Dockerized and hosted on AWS ECS FARGATE
- the micro service saving the number of requests in AWS dynamoDB table
- updating the number of requests with atomic counter (a numeric attribute that is incremented, unconditionally, without interfering with other write requests)
- All components deployed in AWS by Terraform 

- CI/CD was implemented using AWS services codepipeline and codebuild with deployment method of Rolling update. (created manually without Terraform)




