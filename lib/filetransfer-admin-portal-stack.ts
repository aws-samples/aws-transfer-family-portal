// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
import { Stack, StackProps } from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { FargateAppConstruct } from './FargateAppConstruct';
import { NetworkConstruct } from './NetworkConstruct';
import { PrerequisitesConstruct } from './PrerequisitesConstruct';
import {RdsConstruct} from './RdsConstruct';
import {WebappPipelineConstruct} from './WebappPipelineConstruct';
import { TransferAuthFnConstruct } from './sftp-ftps-server/TransferAuthFnConstruct';
import {TransferServerConstruct} from './sftp-ftps-server/TransferServerConstruct';
import {LambdaPipelineConstruct} from './LambdaPipelineConstruct';

export class FiletransferAdminPortalStack extends Stack {

  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);
    const {vpc} = new NetworkConstruct(this, 'fap-network', props);
    const prerequisitesStack = new PrerequisitesConstruct(this, 'fap-prereqs', {vpc: vpc});
    const rdsConstruct = new RdsConstruct(this, 'fap-rds', prerequisitesStack.dbConnectionSg,vpc);
    new LambdaPipelineConstruct(this, 'fap-auth-lambda', vpc);
    const transferAuthFnConstruct = new TransferAuthFnConstruct(this, 'fap-auth-fn', {
      env: props?.env,
      vpc: vpc,
      transferS3Bucket: prerequisitesStack.transferS3Bucket,
      transferPublicKeysS3Bucket: prerequisitesStack.transferPublicKeysS3Bucket,
     // dbCluster: rdsConstruct.dbCluster,
      dbConnectionSg: prerequisitesStack.dbConnectionSg
    });
  
    
    new TransferServerConstruct(this, 'fap-server', {
      env: props?.env,
      vpc: vpc,
      functionArn: transferAuthFnConstruct.transferAuthFn.functionArn,
      domainName: prerequisitesStack.domainName,
      hostedZone: prerequisitesStack.hostedZone,
    }); 


    const {fargateService} = new FargateAppConstruct(this, 'FargateAppStackStack', {
      env: props?.env,
      hostedZone: prerequisitesStack.hostedZone,
      vpc: vpc,
      dbConnectionSg: rdsConstruct.dbConnectionSg,
      dbCluster: rdsConstruct.dbCluster,
      transferS3Bucket: prerequisitesStack.transferS3Bucket,
      transferPublicKeysS3Bucket: prerequisitesStack.transferPublicKeysS3Bucket,
    });
    
    new WebappPipelineConstruct(this, 'fap-codePipeline', {
        vpc: vpc,
        fargateService: fargateService
    });
  }
}
