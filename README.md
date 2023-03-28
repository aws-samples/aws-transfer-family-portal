# Transfer Family Management Web Application	


Stargate, but de-branded, and using cdk v2.

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
   4.  Expand the `Advanced DKIM settings` section.  Select `Easy DKIM` as the `Identity type`, select `RSA_2048_BIT` for the `DKIM signing key length`, and select `Enabled` for both `Publish DNS records to Route 53` and `DKIM signatures`.  Finally, click on the `Create Identity` button.

- **Request SES production access**. SES is in sandbox mode by default, which limits its functionality.  A short request form has to be sent to Amazon.  The response time is usually less than 24 hours.  Follow [these instructions](https://docs.aws.amazon.com/ses/latest/dg/request-production-access.html) to start this process.

## Deployment

The deployment steps, at a high level, are to provision an environment to start the deployment, customize some parameters, and run the deployent. 

- **Provision a Cloud9 instance**  [AWS Cloud9](https://aws.amazon.com/cloud9/) is a cloud-based integrated development environment (IDE) that lets you write and run code within your browser.  This environment is inexpensive and temporary; it should be deleted after a successful deployment.
   1. Navigate to the Cloud9 console and click `Create environment`.  
   2. Give the envionment a meaningful name like "File Transfer Deployer" and click `Next step`.
   3.  Under environment settings, select `Create a new no-ingress EC2 instance for environment (access via Systems Manager)`, select `t3.small` as the instance type, and `Amazon Linux 2` as the Platform. Expand the `Network settings (advanced)` section and make sure to select a subnet with access to an internet or NAT gateway.  Click `Next step`.
   4.  Review your settings and click `Create environment`

- **Clone this repository and initialize the environment**
   1.  Once the Cloud9 environment launches, clone this repository with the command
```
git clone https://gitlab.aws.dev/gmerton/filetransfer-admin-portal.git
```
   
   2.  Run this sequence of commands:
```
cd filetransfer-admin-portal
npm install
cdk bootstrap
```
   3. In the file browser on the left side of the screen, open the file `cdk.context.json` by double-clicking on it.  This is the file where you can customize the application.

- **Set Application Parameters**
   1. `CERTIFICATE_WILDCARD_ARN`.  This is the Amazon Resource Name (ARN) of the wildcard SSL certificate you provisioned in the prerequisites.  Go to the ACM console, copy the ARN of the certificate, and paste it in `cdk.context.json`.  The line should look like this:
` "CERTIFICATE_WILDCARD_ARN": "arn:aws:acm:your-region:123456789:certificate/c89732de-80fd-4b36-af7e-b2dc2596fc2c",`

   2.  `DOMAIN_NAME`.  This is the domain name you registered in the prerequisites.  The line should look like this: `"DOMAIN_NAME": "mydomain.com",`

   3. `VPC_CIDR`.  As part of the deployment, a new VPC is created with the CIDR range defined by this variable.  You can leave this at its default value of `10.195.0.0/21`, or change it to a range of your choosing.

   4. `APP_ADMIN_EMAIL`.  This is the email address that the system will use to send emails to users.  The domain name should match the one you created in earlier steps.  This line will look something like this: `"APP_ADMIN_EMAIL": "admin@mydomain.com",`
  
- **Deploy the application** 
   1. Start the deployment with the command.  The deployment typically takes 15-20 minutes. 
```
cdk deploy --require-approval never
```  

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

## Getting started

To make it easy for you to get started with GitLab, here's a list of recommended next steps.

Already a pro? Just edit this README.md and make it your own. Want to make it easy? [Use the template at the bottom](#editing-this-readme)!

## Add your files

- [ ] [Create](https://docs.gitlab.com/ee/user/project/repository/web_editor.html#create-a-file) or [upload](https://docs.gitlab.com/ee/user/project/repository/web_editor.html#upload-a-file) files
- [ ] [Add files using the command line](https://docs.gitlab.com/ee/gitlab-basics/add-file.html#add-a-file-using-the-command-line) or push an existing Git repository with the following command:

```
cd existing_repo
git remote add origin https://gitlab.aws.dev/gmerton/filetransfer-admin-portal.git
git branch -M main
git push -uf origin main
```

## Integrate with your tools

- [ ] [Set up project integrations](https://gitlab.aws.dev/gmerton/filetransfer-admin-portal/-/settings/integrations)

## Collaborate with your team

- [ ] [Invite team members and collaborators](https://docs.gitlab.com/ee/user/project/members/)
- [ ] [Create a new merge request](https://docs.gitlab.com/ee/user/project/merge_requests/creating_merge_requests.html)
- [ ] [Automatically close issues from merge requests](https://docs.gitlab.com/ee/user/project/issues/managing_issues.html#closing-issues-automatically)
- [ ] [Enable merge request approvals](https://docs.gitlab.com/ee/user/project/merge_requests/approvals/)
- [ ] [Automatically merge when pipeline succeeds](https://docs.gitlab.com/ee/user/project/merge_requests/merge_when_pipeline_succeeds.html)

## Test and Deploy

Use the built-in continuous integration in GitLab.

- [ ] [Get started with GitLab CI/CD](https://docs.gitlab.com/ee/ci/quick_start/index.html)
- [ ] [Analyze your code for known vulnerabilities with Static Application Security Testing(SAST)](https://docs.gitlab.com/ee/user/application_security/sast/)
- [ ] [Deploy to Kubernetes, Amazon EC2, or Amazon ECS using Auto Deploy](https://docs.gitlab.com/ee/topics/autodevops/requirements.html)
- [ ] [Use pull-based deployments for improved Kubernetes management](https://docs.gitlab.com/ee/user/clusters/agent/)
- [ ] [Set up protected environments](https://docs.gitlab.com/ee/ci/environments/protected_environments.html)

***

# Editing this README

When you're ready to make this README your own, just edit this file and use the handy template below (or feel free to structure it however you want - this is just a starting point!).  Thank you to [makeareadme.com](https://www.makeareadme.com/) for this template.

## Suggestions for a good README
Every project is different, so consider which of these sections apply to yours. The sections used in the template are suggestions for most open source projects. Also keep in mind that while a README can be too long and detailed, too long is better than too short. If you think your README is too long, consider utilizing another form of documentation rather than cutting out information.

## Name
Choose a self-explaining name for your project.

## Description
Let people know what your project can do specifically. Provide context and add a link to any reference visitors might be unfamiliar with. A list of Features or a Background subsection can also be added here. If there are alternatives to your project, this is a good place to list differentiating factors.

## Badges
On some READMEs, you may see small images that convey metadata, such as whether or not all the tests are passing for the project. You can use Shields to add some to your README. Many services also have instructions for adding a badge.

## Visuals
Depending on what you are making, it can be a good idea to include screenshots or even a video (you'll frequently see GIFs rather than actual videos). Tools like ttygif can help, but check out Asciinema for a more sophisticated method.

## Installation
Within a particular ecosystem, there may be a common way of installing things, such as using Yarn, NuGet, or Homebrew. However, consider the possibility that whoever is reading your README is a novice and would like more guidance. Listing specific steps helps remove ambiguity and gets people to using your project as quickly as possible. If it only runs in a specific context like a particular programming language version or operating system or has dependencies that have to be installed manually, also add a Requirements subsection.

## Usage
Use examples liberally, and show the expected output if you can. It's helpful to have inline the smallest example of usage that you can demonstrate, while providing links to more sophisticated examples if they are too long to reasonably include in the README.

## Support
Tell people where they can go to for help. It can be any combination of an issue tracker, a chat room, an email address, etc.

## Roadmap
If you have ideas for releases in the future, it is a good idea to list them in the README.

## Contributing
State if you are open to contributions and what your requirements are for accepting them.

For people who want to make changes to your project, it's helpful to have some documentation on how to get started. Perhaps there is a script that they should run or some environment variables that they need to set. Make these steps explicit. These instructions could also be useful to your future self.

You can also document commands to lint the code or run tests. These steps help to ensure high code quality and reduce the likelihood that the changes inadvertently break something. Having instructions for running tests is especially helpful if it requires external setup, such as starting a Selenium server for testing in a browser.

## Authors and acknowledgment
Show your appreciation to those who have contributed to the project.

## License
For open source projects, say how it is licensed.

## Project status
If you have run out of energy or time for your project, put a note at the top of the README saying that development has slowed down or stopped completely. Someone may choose to fork your project or volunteer to step in as a maintainer or owner, allowing your project to keep going. You can also make an explicit request for maintainers.
=======
# Welcome to your CDK TypeScript project

This is a blank project for CDK development with TypeScript.

The `cdk.json` file tells the CDK Toolkit how to execute your app.

## Useful commands

* `npm run build`   compile typescript to js
* `npm run watch`   watch for changes and compile
* `npm run test`    perform the jest unit tests
* `cdk deploy`      deploy this stack to your default AWS account/region
* `cdk diff`        compare deployed stack with current state
* `cdk synth`       emits the synthesized CloudFormation template
>>>>>>> master
