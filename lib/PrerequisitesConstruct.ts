// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
import {Construct} from "constructs";
import {aws_s3 as s3, aws_route53 as route53, aws_ssm as ssm} from "aws-cdk-lib";
import {Stack, StackProps, RemovalPolicy} from "aws-cdk-lib";
import { createHash } from 'crypto';


export class PrerequisitesConstruct extends Construct {
      public readonly transferS3Bucket: s3.Bucket;
      public readonly transferPublicKeysS3Bucket: s3.Bucket;
      public readonly hostedZone?: route53.IHostedZone;
      public readonly domainName?: string;
    
      constructor(scope: Stack, id: string, props?: StackProps) {
        super(scope, id);
    
        const appAdminEmail = this.node.tryGetContext('APP_ADMIN_EMAIL');
      
        //Create a bucket that acts as the backend for Transfer Family.
        this.transferS3Bucket = new s3.Bucket(this, 'transferS3Bucket', {
          accessControl: s3.BucketAccessControl.PRIVATE,
          encryption: s3.BucketEncryption.S3_MANAGED,
          versioned: true,
          blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
          removalPolicy: RemovalPolicy.DESTROY,
          autoDeleteObjects: true,
        });
    
    
        //Create a separate bucket that holds the public key of user key pairs.
        this.transferPublicKeysS3Bucket = new s3.Bucket(this, 'transferPublicKeysS3Bucket', {
          accessControl: s3.BucketAccessControl.PRIVATE,
          encryption: s3.BucketEncryption.S3_MANAGED,
          versioned: false,
          blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
          removalPolicy: RemovalPolicy.DESTROY,
          autoDeleteObjects: true,
        });
    
    
        //Store the data bucket name in parameter store
        new ssm.StringParameter(this, 'transferS3BucketNameParameter', {
          parameterName: '/Applications/FileTransferAdminPortal/S3-Storage-Bucket-Name',
          stringValue: this.transferS3Bucket.bucketName,
        }).applyRemovalPolicy(RemovalPolicy.DESTROY);

        //Store the data bucket arn in parameter store
        new ssm.StringParameter(this, 'transferS3BucketArnParameter', {
          parameterName: '/Applications/FileTransferAdminPortal/S3-Storage-Bucket-ARN',
          stringValue: this.transferS3Bucket.bucketArn,
        }).applyRemovalPolicy(RemovalPolicy.DESTROY);
        
        //Store the key bucket name in parameter store
        new ssm.StringParameter(this, 'transferPublicKeysS3BucketNameParameter', {
          parameterName: '/Applications/FileTransferAdminPortal/S3-Keypair-Bucket-Name',
          stringValue: this.transferPublicKeysS3Bucket.bucketName,
        }).applyRemovalPolicy(RemovalPolicy.DESTROY);
        
        
        //Store the key bucket arn in parameter store.
        new ssm.StringParameter(this, 'transferPublicKeysS3BucketArnParameter', {
          parameterName: '/Applications/FileTransferAdminPortal/S3-Keypair-Bucket-ARN',
          stringValue: this.transferPublicKeysS3Bucket.bucketArn,
        }).applyRemovalPolicy(RemovalPolicy.DESTROY);
        
        
        //Store the app's email sender address in parameter store
        new ssm.StringParameter(this, 'SenderEmailAddressParameter', {
          parameterName: '/Applications/FileTransferAdminPortal/sender-email-address',
          stringValue: appAdminEmail,
        }).applyRemovalPolicy(RemovalPolicy.DESTROY);
      
    }}
    