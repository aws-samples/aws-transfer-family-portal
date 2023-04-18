// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import {aws_ec2 as ec2, aws_lambda as lambda, aws_iam as iam, custom_resources as cr, Duration,Stack} from 'aws-cdk-lib'
import { Construct } from 'constructs';
import * as logs from 'aws-cdk-lib/aws-logs';
import { createHash } from 'crypto'


export interface CdkResourceInitializerProps {
  vpc: ec2.IVpc
  subnetsSelection: ec2.SubnetSelection
  fnSecurityGroups: ec2.ISecurityGroup[]
  fnTimeout: Duration
  fnCode: lambda.DockerImageCode
  fnLogRetention: logs.RetentionDays
  fnMemorySize?: number,
  config: any
}

export class CdkResourceInitializer extends Construct {
  public readonly response: string
  public readonly customResource: cr.AwsCustomResource
  public readonly function: lambda.Function

  constructor (scope: Construct, id: string, props: CdkResourceInitializerProps) {
    super(scope, id)

    const stack = Stack.of(this)

    const fnSg = new ec2.SecurityGroup(this, 'ResourceInitializerFnSg', {
      securityGroupName: `${id}ResourceInitializerFnSg`,
      vpc: props.vpc,
      allowAllOutbound: true
    })

      const fn= new lambda.DockerImageFunction(this, 'ResourceInitializerFn', {
        memorySize: props.fnMemorySize || 128,
        functionName: `${id}-ResInit${stack.stackName}`,
        code: props.fnCode,
        vpcSubnets: props.vpc.selectSubnets(props.subnetsSelection),
        vpc: props.vpc,
        securityGroups: [fnSg, ...props.fnSecurityGroups],
        timeout: props.fnTimeout,
        logRetention: props.fnLogRetention,
        allowAllOutbound: true
    })
     const createSecretPolicyStatement = new iam.PolicyStatement({
          resources: ['*'],
          actions: ['secretsmanager:CreateSecret'],
          effect: iam.Effect.ALLOW,
        })
    fn.role?.attachInlinePolicy(
      new iam.Policy(this, 'createSecretPolicy', {
        statements: [createSecretPolicyStatement],
      }),
    );
    
    const payload: string = JSON.stringify({
      params: {
        config: props.config
      }
    })

    const payloadHashPrefix = createHash('md5').update(payload).digest('hex').substring(0, 6)

    const sdkCall: cr.AwsSdkCall = {
      service: 'Lambda',
      action: 'invoke',
      parameters: {
        FunctionName: fn.functionName,
        Payload: payload
      },
      physicalResourceId: cr.PhysicalResourceId.of(`${id}-AwsSdkCall-${fn.currentVersion.version + payloadHashPrefix}`)
    
    }
    
    const customResourceFnRole = new iam.Role(this, 'AwsCustomResourceRole', {
      assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com')
    })
    customResourceFnRole.addToPolicy(
      new iam.PolicyStatement({
        resources: [`arn:aws:lambda:${stack.region}:${stack.account}:function:*-ResInit${stack.stackName}`],
        actions: ['lambda:InvokeFunction']
      })
    )
    
   
    
    this.customResource = new cr.AwsCustomResource(this, 'AwsCustomResource', {
      policy: cr.AwsCustomResourcePolicy.fromSdkCalls({ resources: cr.AwsCustomResourcePolicy.ANY_RESOURCE }),
      onUpdate: sdkCall,
      timeout: Duration.minutes(10),
      role: customResourceFnRole
    })

    this.response = this.customResource.getResponseField('Payload')
    this.function = fn
  }
}