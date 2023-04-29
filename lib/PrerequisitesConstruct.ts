// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
import {Construct} from "constructs";

import {Bucket, BucketAccessControl, BucketEncryption, BlockPublicAccess} from "aws-cdk-lib/aws-s3";
import {Vpc} from "aws-cdk-lib/aws-ec2";
import {IHostedZone} from "aws-cdk-lib/aws-route53";
import {Stack, StackProps, RemovalPolicy} from "aws-cdk-lib";
//import { createHash } from 'crypto';
import {SecurityGroup, Peer, Port} from "aws-cdk-lib/aws-ec2";
import {StringParameter} from "aws-cdk-lib/aws-ssm";
 export interface PrerequisitesConstructProps extends StackProps {
      readonly vpc: Vpc
      }


export class PrerequisitesConstruct extends Construct {
      public readonly transferS3Bucket: Bucket;
      public readonly transferPublicKeysS3Bucket: Bucket;
      public readonly hostedZone?: IHostedZone;
      public readonly domainName?: string;
      public readonly dbConnectionSg:SecurityGroup
      readonly vpc:Vpc
     
      
      constructor(scope: Stack, id: string,  props:PrerequisitesConstructProps) {
        super(scope, id);
        this.vpc=props.vpc;
        const appAdminEmail = this.node.tryGetContext('APP_ADMIN_EMAIL');
      
        //Create a bucket that acts as the backend for Transfer Family.
        this.transferS3Bucket = new Bucket(this, 'transferS3Bucket', {
          accessControl: BucketAccessControl.PRIVATE,
          encryption: BucketEncryption.S3_MANAGED,
          versioned: true,
          blockPublicAccess: BlockPublicAccess.BLOCK_ALL,
          removalPolicy: RemovalPolicy.DESTROY,
          autoDeleteObjects: true,
        });
    
    
        //Create a separate bucket that holds the public key of user key pairs.
        this.transferPublicKeysS3Bucket = new Bucket(this, 'transferPublicKeysS3Bucket', {
          accessControl: BucketAccessControl.PRIVATE,
          encryption: BucketEncryption.S3_MANAGED,
          versioned: false,
          blockPublicAccess: BlockPublicAccess.BLOCK_ALL,
          removalPolicy: RemovalPolicy.DESTROY,
          autoDeleteObjects: true,
        });
    
    
        //Store the data bucket name in parameter store
        new StringParameter(this, 'transferS3BucketNameParameter', {
          parameterName: '/Applications/FileTransferAdminPortal/S3-Storage-Bucket-Name',
          stringValue: this.transferS3Bucket.bucketName,
        }).applyRemovalPolicy(RemovalPolicy.DESTROY);

        //Store the data bucket arn in parameter store
        new StringParameter(this, 'transferS3BucketArnParameter', {
          parameterName: '/Applications/FileTransferAdminPortal/S3-Storage-Bucket-ARN',
          stringValue: this.transferS3Bucket.bucketArn,
        }).applyRemovalPolicy(RemovalPolicy.DESTROY);
        
        //Store the key bucket name in parameter store
        new StringParameter(this, 'transferPublicKeysS3BucketNameParameter', {
          parameterName: '/Applications/FileTransferAdminPortal/S3-Keypair-Bucket-Name',
          stringValue: this.transferPublicKeysS3Bucket.bucketName,
        }).applyRemovalPolicy(RemovalPolicy.DESTROY);
        
        
        //Store the key bucket arn in parameter store.
        new StringParameter(this, 'transferPublicKeysS3BucketArnParameter', {
          parameterName: '/Applications/FileTransferAdminPortal/S3-Keypair-Bucket-ARN',
          stringValue: this.transferPublicKeysS3Bucket.bucketArn,
        }).applyRemovalPolicy(RemovalPolicy.DESTROY);
        
        
        //Store the app's email sender address in parameter store
        new StringParameter(this, 'SenderEmailAddressParameter', {
          parameterName: '/Applications/FileTransferAdminPortal/sender-email-address',
          stringValue: appAdminEmail,
        }).applyRemovalPolicy(RemovalPolicy.DESTROY);
        
      this.dbConnectionSg = new SecurityGroup(this, "DB-securitygroup", {
        description: "File Transfer Admin Portal DB Security Group",
        vpc: this.vpc,
        securityGroupName: "FileTransferAdminPortalDB-SG"
      })
    
      this.dbConnectionSg.addIngressRule(
        Peer.anyIpv4(),
        Port.tcp(3306),
        "Allow inbound"
      )
        
      
    }}
    