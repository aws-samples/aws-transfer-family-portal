# Transfer Family Management Web Application	
This web application allows administrators of a Transfer Family instance to easily manage users and their access rights. Features include:

- Users can be created, edited and deactivated by administrators.
- Administrators can create directory mappings and configure users' access privileges to them.
- Users can be grouped into organizations.
- Administrators can allow users to authenticate with either a password, a key pair, or both.
- Users can upload and download files through the web application or via a third-party SFTP or FTPS client.
- A CI/CD pipeline so that administrators of this application can customize the solution and quickly deploy the changes to production.

At an operational level, the solution is packaged and deployed as an AWS CDK package.  The package deploys all the architectural components the solution needs to function.  The most important components include are listed below; see the architectural diagram for more details.

- An AWS Transfer Family instance using S3 as its data store.
- A containerized web application hosted on AWS Fargate.
- An Aurora RDS instance that supports the web application.

## Architecture Diagram

![file-transfer-architecture (1)](https://user-images.githubusercontent.com/127906259/234111910-d62120d5-44d5-4c4c-b5ec-2196b6aa91bb.svg)

## CI/CD
Besides the components required for the application to function, the CDK also deploys CI/CD pipelines so that adopters of this solution can easily customize the solution. The deployment creates two CodeCommit repositories: one for the web application's code, and one for that of the authentication code.


### Web Application Pipeline
 The components that support this flow are:

- The CDK creates a CodeCommit repoistory and populates it with the code for the web applications.
- An AWS CodePipeline is provisioned.  This pipeline uses the CodeCommit repository as its source, along with AWS CodeBuild and AWS CodeDeploy stages to build and deploy the changes.
- Whenever a user commits to the master branch of the repository, AWS CodeBuild performs several steps with the output being a docker image.
- The docker image is written to an Amazon Elastic Container Registry (ECR) repository.
- Finally, AWS CodeDeploy creates a new task definition revision, and deploys the new image to AWS Fargate

![file-transfer-pipeline](https://user-images.githubusercontent.com/127906259/234119132-680363e6-9be5-4f91-a7cd-f8e482221b93.svg)

### Authentication Lambda Pipeline
The components and workflow for the authentication lambda are similar to that of the web application, the main exception being there is no CodeDeploy step.  Instead, the deployment of a fresh docker image to AWS Lambda is handled by AWS CodeBuild.

- The CDK creates a CodeCommit repoistory and populates it with the code for the authentication lambda.
- An AWS CodePipeline is provisioned.  This pipeline uses the CodeCommit repository as its source, along with AWS CodeBuildto build and deploy the changes.
- Whenever a user commits to the master branch of the repository, the pipeline is triggered.
- The docker image is written to an Amazon Elastic Container Registry (ECR) repository by AWS CodeBuild.
- AWS CodeBuild updates the authentication lambda with the newly created image.

![stargate-auth-cicd (1)](https://github.com/aws-samples/aws-transfer-family-portal/assets/127906259/1a10a0be-7e1d-4656-87ad-dfb8e3d762f3)

## Prerequisites

The majority of the web application's architectural components are automatically deployed by the CDK package, but there are a few manual items that need to be configured before running it.


- **Region.**  Choose an AWS region to host the web application.  During the deployment, make sure that all instructions are followed within your desired region.  As of this writing, this solution has been tested successfully in the us-east-1, us-east-2, us-west-1 and us-west-2 regions.

- **Sufficient Elastic IP's.** The solution requires three Elastic IPs (EIP) within your target region.  Navigate to the EC2 console; the number of EIPs currently in use by the region is shown in the inventory table at the top of the screen.  To view the region's EIP limit, navigate to the Service Quota tab.  Select the service Amazon EC2 and enter Elastic IP in the search bar.  If the quota minus the number currently in use is greater than or equal to three, then your region's capacity is sufficient.

- **Public Hosted Zone.**  Your users will interface with the web application by navigating to a URL using your domain name, `mydomain.com`, for example.   A public hosted zone is a container that holds information for a specific domain.  When you registrer a domain with Route 53, a public hosted zone is created for you.  Follow [these instructions](https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/domain-register.html) to register a domain with Route 53.

- **AWS SSL/TLS Certificate.** To ensure encryption in transit, use AWS Certificate Manager to [provision a public certificate](https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-request-public.html).  When provisioning, set the full qualified domain name to `*.mydomain.com`, using the domain name you registered in Route 53.  Be sure to provision the certificate in your desired region.

- **Configure SES**. The web application uses Amazon Simple Email Service (SES) to send administrative emails, for example, to send temporary passwords or to notify users of important events.  While you can use SES out of the box, there are some tweaks that can be made so that receiving email servers will trust your system's emails and not classify them as spam.  Make sure to follow the steps below within your desired region.
   1. Within the SES console navigate to `Verified Identities` and click on `Create Identity`
   2. For `Identity type` select `Domain`.  Enter the domain you provisioned earlier, e.g., `mydomain.com`
   3.  Check the box labeled `Use a custom MAIL FROM domain` and enter `mailfrom` in the `MAIL FROM domain` textbox.  Select `Enabled` under `Publish DNS records to Route 53`.
   4.  Expand the `Advanced DKIM settings` section.  Select `Easy DKIM` as the `Identity type`, select `RSA_2048_BIT` for the `DKIM signing key length`, and select `Enabled` for both `Publish DNS records to Route 53` and `DKIM signatures`.  Finally, click on the `Create Identity` button.  The next screen will show several DNS records.  Open another tab and navigate to Route 53.  Verify that the DNS records on the SES screen are listed with your other DNS records.  After a couple minutes, refresh the SES screen and the status should change from Pending to Verified.
 


- **Request SES production access**. SES is in sandbox mode by default, which limits its functionality.  A short request form has to be sent to Amazon.  The response time is usually less than 24 hours.  Follow [these instructions](https://docs.aws.amazon.com/ses/latest/dg/request-production-access.html) to start this process.

## Deployment

The deployment steps, at a high level, are to provision an environment to start the deployment, customize some parameters, and run the deployent. 

**Provision a Cloud9 instance**  [AWS Cloud9](https://aws.amazon.com/cloud9/) is a cloud-based integrated development environment (IDE) that lets you write and run code within your browser.  This environment is inexpensive and temporary; it should be deleted after a successful deployment.
   1. Navigate to the Cloud9 console and click `Create environment`.  
   2. Give the envionment a meaningful name like "File Transfer Deployer" and click `Next step`.
   3.  Under environment settings, select `Create a new no-ingress EC2 instance for environment (access via Systems Manager)`, select `t3.small` as the instance type, and `Amazon Linux 2` as the Platform. Expand the `Network settings (advanced)` section and make sure to select a subnet with access to an internet or NAT gateway.  Click `Next step`.
   4.  Review your settings and click `Create environment`

**Clone this repository and initialize the environment**
   1.  Once the Cloud9 environment launches, clone this repository with the command
```
git clone https://github.com/aws-samples/aws-transfer-family-portal.git
```
   
   2.  Run this sequence of commands:
```
cd aws-transfer-family-portal
npm install
cdk bootstrap
```
   3. In the file browser on the left side of the screen, open the file `cdk.context.json` by double-clicking on it.  This is the file where you can customize the application.

**Set Application Parameters**
   1. `CERTIFICATE_WILDCARD_ARN`.  This is the Amazon Resource Name (ARN) of the wildcard TLS certificate you provisioned in the prerequisites.  Go to the ACM console, copy the ARN of the certificate, and paste it in `cdk.context.json`.  The line should look like this:
` "CERTIFICATE_WILDCARD_ARN": "arn:aws:acm:your-region:123456789:certificate/c89732de-80fd-4b36-af7e-b2dc2596fc2c",`

   2.  `DOMAIN_NAME`.  This is the domain name you registered in the prerequisites.  The line should look like this: `"DOMAIN_NAME": "mydomain.com",`

   3. `VPC_CIDR`.  As part of the deployment, a new VPC is created with the CIDR range defined by this variable.  You can leave this at its default value of `10.195.0.0/21`, or change it to a range of your choosing.

   4. `APP_ADMIN_EMAIL`.  This is the email address that the system will use to send emails to users.  The domain name should match the one you created in earlier steps.  This line will look something like this: `"APP_ADMIN_EMAIL": "admin@mydomain.com",`

   5. Save your changes to `cdk.context.json` and close the file.
  
**Deploy the application** 
   1. Start the deployment with the command below.  The deployment typically takes 15-20 minutes. 

```cdk deploy --require-approval never```  

   2. Once the deployment is complete, navigate to the ECS console.  Under `Clusters` select the newly created cluster.  Its name will start with "FiletransferAdminPortalStack."

   3.  In the `Services` tab, there will be a single service.  Click on it, and then click `Edit service`.  Change the value of `Desired tasks` from 0 to 1 and click `Update`.

   4.  Once the `Tasks` status is green with a label '1 Running', it is time to login to the app.

   5.  Navigate to the CloudFormation console, select the `FiletransferAdminPortalStack` and go to the `Outputs` tab.  Click on the link with the description "Web App Endpoint."  This will take you to the login screen.

   6.  To find the initial user's credentials, navigate to Secrets Manager.  Select the `FileTransferPortalInitialCreds` secret and then click on `Retrieve secret value`.  Use this username and password to login to the application.

   7.  Once the application is successfully deployed, the Cloud9 environment is no longer necessary.  Go to the Cloud9 console and delete it.

## Deleting the Application
If you'd like to remove the application from your environment, almost all of the components will be removed by deleting the stack in CloudFormation.  There are just a couple of manual steps that need to be run in order for the delete to execute cleanly.

   1.  Navigate to the ECS console and select the FiletransferAdminPortal cluster.  Select the single service, and click `Edit service`.  Change `Desired tasks` from 1 to 0.

   2.  Navagite to the Elastic Container Repository (ECR) console, and select the FiletransferAdminPortal repository.  Delete all images in the repo.

   3.  If there are any other architectural components in the application VPC that were manually created, they will need to be manually deleted.

   4.  Navigate to the CloudFormation console and delete the FiletransferAdminPortalStack.

