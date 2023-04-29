// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import {Construct} from 'constructs';
import {aws_ec2 as ec2, aws_lambda as lambda, aws_iam as iam, aws_s3 as s3, aws_rds as rds, aws_logs as logs, aws_ssm as ssm} from 'aws-cdk-lib';
import {StackProps, Stack, RemovalPolicy, ArnFormat, Duration} from 'aws-cdk-lib';

export interface TransferAuthFnStackProps extends StackProps{
  readonly vpc: ec2.IVpc;
  readonly transferS3Bucket: s3.IBucket;
  readonly transferPublicKeysS3Bucket: s3.IBucket;
  //readonly dbCluster: rds.IDatabaseCluster;
  readonly dbConnectionSg: ec2.ISecurityGroup;
}

export class TransferAuthFnConstruct extends Construct {
  public readonly transferAuthFn: lambda.IFunction;

  constructor(scope: Stack, id: string, props: TransferAuthFnStackProps) {
    super(scope, id);
    
    // Role given by the transfer server's auth function to authenticated user
    const transferS3AccessRole = new iam.Role(this, 'TransferS3AccessRole', {
      assumedBy: new iam.ServicePrincipal('transfer.amazonaws.com'),
    });

    props.transferS3Bucket.grantReadWrite(transferS3AccessRole);
    props.transferPublicKeysS3Bucket.grantRead(transferS3AccessRole);

    const transferAuthFnSg = new ec2.SecurityGroup(this, 'TransferAuthFnSg', {
      vpc: props.vpc,
      allowAllOutbound: true,
    });

    transferAuthFnSg.applyRemovalPolicy(RemovalPolicy.DESTROY);

    transferAuthFnSg.addIngressRule(
      ec2.Peer.ipv4(props.vpc.vpcCidrBlock),
      ec2.Port.allTraffic(),
    );

  

    this.transferAuthFn = new lambda.DockerImageFunction(this, 'TransferAuthFn', {
      description: 'A function to provide IAM roles and policies for given user and serverId.',
      memorySize: 512,
      code: lambda.DockerImageCode.fromImageAsset(`${__dirname}/transfer-auth-fn-code`, {}),
      vpcSubnets: props.vpc.selectSubnets({
        subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
      }),
      vpc: props.vpc,
      securityGroups: [transferAuthFnSg],
      timeout: Duration.seconds(30),
      logRetention: logs.RetentionDays.FIVE_MONTHS,
      allowAllOutbound: true,
      functionName: "TransferFamilyAuth"
    });

    props.transferPublicKeysS3Bucket.grantRead(this.transferAuthFn);

    const dbImportedSecurityGroupTransferAuth = ec2.SecurityGroup.fromSecurityGroupId(
      this,
      'DbImportedSecurityGroupTransferAuth',
      
      props.dbConnectionSg.securityGroupId,
      { allowAllOutbound: true, mutable: true },
    );

    dbImportedSecurityGroupTransferAuth.addIngressRule(
      transferAuthFnSg,
      ec2.Port.tcp(3306),
    );

 const fnInitialExecutionPolicy = new iam.Policy(this, 'FnInitialExecutionPolicy', {
      statements: [
        new iam.PolicyStatement({
          effect: iam.Effect.ALLOW,
          resources: ["*"],
          actions: ['rds-db:connect'],
        }),
        new iam.PolicyStatement({
          effect: iam.Effect.ALLOW,
          resources: ["*"],
          
          actions: [
            'ssm:Describe*',
            'ssm:Get*',
            'ssm:List*',
          ],
        }),
      ],
    });

    fnInitialExecutionPolicy.attachToRole(this.transferAuthFn.role!);

    this.transferAuthFn.grantInvoke(new iam.ServicePrincipal('transfer.amazonaws.com'));

    /* eslint-disable no-new */
    new ssm.StringParameter(this, 'TransferS3AccessRoleParameter', {
      parameterName: '/Applications/FileTransferAdminPortal/TransferS3AccessRole',
      stringValue: transferS3AccessRole.roleArn,
    });
    new ssm.StringParameter(this, 'LambdaAuthNameParameter', {
      parameterName: '/Applications/FileTransferAdminPortal/LambdaAuthName',
      stringValue: this.transferAuthFn.functionName,
    });
  }
}
