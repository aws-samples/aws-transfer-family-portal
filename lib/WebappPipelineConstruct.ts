// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import { Pipeline, Artifact } from "aws-cdk-lib/aws-codepipeline"
import { PipelineProject, Project } from "aws-cdk-lib/aws-codebuild"
import {Construct} from 'constructs'
import { CodeCommitSourceAction } from "aws-cdk-lib/aws-codepipeline-actions"
import { CodeBuildAction } from "aws-cdk-lib/aws-codepipeline-actions"
import { EcsDeployAction } from "aws-cdk-lib/aws-codepipeline-actions"
import {Effect, PolicyStatement} from 'aws-cdk-lib/aws-iam'
import { LinuxBuildImage} from "aws-cdk-lib/aws-codebuild"
import {IVpc, SubnetType} from 'aws-cdk-lib/aws-ec2'
import {Repository, Code} from 'aws-cdk-lib/aws-codecommit'
import {StackProps} from 'aws-cdk-lib';
import {FargateService} from 'aws-cdk-lib/aws-ecs'

export interface PipelineProps extends StackProps {
  readonly vpc: IVpc;
  readonly fargateService: FargateService;
  
}

export class WebappPipelineConstruct extends Construct {
readonly fargateService:FargateService;
constructor(scope: Construct, id: string, props: PipelineProps) {
    super(scope, id);
    this.fargateService = props.fargateService;
    
    const pipeline = new Pipeline(this, "pipeline", {crossAccountKeys: false})
    const sourceOutput = new Artifact();
    const codeBuildOutput = new Artifact();
    const codeCommitRepo = new Repository(this, 'codeCommitRepo', {
      repositoryName: "file-transfer-admin-portal",
      //code: Code.fromDirectory(`${__dirname}/web-app`, 'master'), // optional property, branch parameter can be omitted
      code: Code.fromDirectory(`${__dirname}/fargate-app/web-app`, 'master'), // optional property, branch parameter can be omitted
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
  vpc:props.vpc,
  subnetSelection: props.vpc.selectSubnets( {
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
  outputs: [codeBuildOutput],
  input: sourceOutput
})

pipeline.addStage({
  stageName:"Build",
  actions: [codeBuildAction]
})



  
    const ecsDeployAction = new EcsDeployAction({
      actionName: "EcsDeploymentAction",
      service:this.fargateService,
      input: codeBuildOutput
    })
    
    pipeline.addStage({
      stageName: "ECS-Deploy",
      actions: [ecsDeployAction]
    })
}
}
