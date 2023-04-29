// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import {Construct} from 'constructs';
//import {aws_ec2 as ec2, aws_lambda as lambda, aws_iam as iam, aws_s3 as s3, aws_rds as rds, aws_logs as logs, aws_ssm as ssm} from 'aws-cdk-lib';
import {StackProps, Stack, RemovalPolicy, ArnFormat, Duration} from 'aws-cdk-lib';
import {IVpc, ISecurityGroup, Peer,Port, SecurityGroup, Subnet, SubnetType} from 'aws-cdk-lib/aws-ec2';
import {Bucket, IBucket} from 'aws-cdk-lib/aws-s3';
import {IFunction,DockerImageFunction, DockerImageCode} from 'aws-cdk-lib/aws-lambda';
import {Role, ServicePrincipal, Policy, PolicyStatement, Effect} from 'aws-cdk-lib/aws-iam';
import {RetentionDays} from 'aws-cdk-lib/aws-logs';
import {StringParameter} from 'aws-cdk-lib/aws-ssm';

export interface TransferAuthFnStackProps extends StackProps{
  readonly vpc: IVpc;
  readonly transferS3Bucket: IBucket;
  readonly transferPublicKeysS3Bucket: IBucket;
  //readonly dbCluster: rds.IDatabaseCluster;
  readonly dbConnectionSg: ISecurityGroup;
}

export class TransferAuthFnConstruct extends Construct {
  public readonly transferAuthFn: IFunction;

  constructor(scope: Stack, id: string, props: TransferAuthFnStackProps) {
    super(scope, id);
    
    // Role given by the transfer server's auth function to authenticated user
    const transferS3AccessRole = new Role(this, 'TransferS3AccessRole', {
      assumedBy: new ServicePrincipal('transfer.amazonaws.com'),
    });

    props.transferS3Bucket.grantReadWrite(transferS3AccessRole);
    props.transferPublicKeysS3Bucket.grantRead(transferS3AccessRole);

    const transferAuthFnSg = new SecurityGroup(this, 'TransferAuthFnSg', {
      vpc: props.vpc,
      allowAllOutbound: true,
    });

    transferAuthFnSg.applyRemovalPolicy(RemovalPolicy.DESTROY);

    transferAuthFnSg.addIngressRule(
      Peer.ipv4(props.vpc.vpcCidrBlock),
      Port.allTraffic(),
    );

  

    this.transferAuthFn = new DockerImageFunction(this, 'TransferAuthFn', {
      description: 'Transfer Family Authorization function.',
      memorySize: 512,
      code: DockerImageCode.fromImageAsset(`${__dirname}/transfer-auth-fn-code`, {}),
      vpcSubnets: props.vpc.selectSubnets({
        subnetType: SubnetType.PRIVATE_ISOLATED,
      }),
      vpc: props.vpc,
      securityGroups: [transferAuthFnSg],
      timeout: Duration.seconds(30),
      logRetention: RetentionDays.FIVE_MONTHS,
      allowAllOutbound: true,
      functionName: "TransferFamilyAuth"
    });


  
    props.transferPublicKeysS3Bucket.grantRead(this.transferAuthFn);

    const dbImportedSecurityGroupTransferAuth = SecurityGroup.fromSecurityGroupId(
      this,
      'DbImportedSecurityGroupTransferAuth',
      
      props.dbConnectionSg.securityGroupId,
      { allowAllOutbound: true, mutable: true },
    );

    dbImportedSecurityGroupTransferAuth.addIngressRule(
      transferAuthFnSg,
      Port.tcp(3306),
    );

 const fnInitialExecutionPolicy = new Policy(this, 'FnInitialExecutionPolicy', {
      statements: [
        new PolicyStatement({
          effect: Effect.ALLOW,
          resources: ["*"],
          actions: ['rds-db:connect'],
        }),
        new PolicyStatement({
          effect: Effect.ALLOW,
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

    this.transferAuthFn.grantInvoke(new ServicePrincipal('transfer.amazonaws.com'));
  
    
    new StringParameter(this, 'TransferS3AccessRoleParameter', {
      parameterName: '/Applications/FileTransferAdminPortal/TransferS3AccessRole',
      stringValue: transferS3AccessRole.roleArn,
    });
    new StringParameter(this, 'LambdaAuthNameParameter', {
      parameterName: '/Applications/FileTransferAdminPortal/LambdaAuthName',
      stringValue: this.transferAuthFn.functionName,
    });
  }
}
