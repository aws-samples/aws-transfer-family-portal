// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import { Pipeline, Artifact } from "aws-cdk-lib/aws-codepipeline";
import { PipelineProject } from "aws-cdk-lib/aws-codebuild";
import {Construct} from 'constructs';
import { CodeCommitSourceAction } from "aws-cdk-lib/aws-codepipeline-actions";
import { CodeBuildAction } from "aws-cdk-lib/aws-codepipeline-actions";
import {Effect, PolicyStatement} from 'aws-cdk-lib/aws-iam';
import { LinuxBuildImage} from "aws-cdk-lib/aws-codebuild";
import {IVpc, SubnetType} from 'aws-cdk-lib/aws-ec2';
import {Repository, Code} from 'aws-cdk-lib/aws-codecommit';
import {Stack, RemovalPolicy} from 'aws-cdk-lib';
import {Repository as EcrRepository} from 'aws-cdk-lib/aws-ecr';
import {StringParameter, ParameterDataType} from 'aws-cdk-lib/aws-ssm';
/*
export interface PipelineProps extends StackProps {
  readonly vpc: IVpc;
  readonly fargateService: FargateService;
  
}
*/
export class LambdaPipelineConstruct extends Construct {
//readonly fargateService:FargateService;
constructor(scope: Construct, id: string, vpc: IVpc) {
    super(scope, id);
   // this.fargateService = props.fargateService;
    const pipeline = new Pipeline(this, "lambda_pipeline", {crossAccountKeys: false})
    const sourceOutput = new Artifact();
    const account = Stack.of(this).account
    const region = Stack.of(this).region
//const codeBuildOutput = new Artifact();
    
    /****ECR******* */

const ecrRepository = new EcrRepository(this, "ecr", {
  removalPolicy: RemovalPolicy.DESTROY
})

new StringParameter(this, "ecr-uri", {
  dataType: ParameterDataType.TEXT,
  parameterName: "/Applications/FileTransferAdminPortal/lambda-auth-ecr-uri",
  stringValue: ecrRepository.repositoryUri
})


const ecrRepoEndpoint = account + ".dkr.ecr." + region +".amazonaws.com"


new StringParameter(this, "ecr-endpoint", {
  dataType: ParameterDataType.TEXT,
  parameterName: "/Applications/FileTransferAdminPortal/lambda-auth-ecr-endpoint",
  stringValue: ecrRepoEndpoint
})
    const codeCommitRepo = new Repository(this, 'lambdaCodeCommitRepo', {
      repositoryName: "file-transfer-lambda-auth",
      code: Code.fromDirectory(`${__dirname}/sftp-ftps-server/transfer-auth-fn-code`, 'master'), // optional property, branch parameter can be omitted
    });
    
    
    const codeCommitSourceAction = new CodeCommitSourceAction({
      actionName: "Source-Action",
      branch: "master", 
      output: sourceOutput,
      repository: codeCommitRepo

    })
  
    pipeline.addStage({
      stageName: "Source-Stage",
      actions: [codeCommitSourceAction]
    })
  

  
  const project = new PipelineProject(this, "pipelineproject", {
    vpc:vpc,
    subnetSelection: vpc.selectSubnets( {
    subnetType:SubnetType.PRIVATE_WITH_NAT
    }),
    environment: {
      privileged: true,
      buildImage: LinuxBuildImage.STANDARD_7_0
    }
  })

  const codeBuildPolicyStatement = new PolicyStatement( {
    sid: "FileTransferCodeBuild",
    effect: Effect.ALLOW,
    resources: ["*"],
    actions: ["codebuild:*", "ssm:GetParameters", "ecr:*"]
})

  project.addToRolePolicy(codeBuildPolicyStatement)

const codeBuildAction = new CodeBuildAction({
  actionName:"BuildAction",
  project:project,
//  outputs: [codeBuildOutput],
  input: sourceOutput
})

pipeline.addStage({
  stageName:"Build",
  actions: [codeBuildAction]
})
/*
const ecsDeployAction = new EcsDeployAction({
      actionName: "EcsDeploymentAction",
      service:this.fargateService,
      input: codeBuildOutput
    })
    
    pipeline.addStage({
      stageName: "ECS-Deploy",
      actions: [ecsDeployAction]
    })
    */
  }
}
