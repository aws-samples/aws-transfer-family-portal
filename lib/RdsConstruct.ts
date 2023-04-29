// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

//import{aws_ec2 as ec2, aws_rds as rds, aws_ssm as ssm, aws_iam as iam, aws_secretsmanager as secretsmanager}  from 'aws-cdk-lib'
import {Stack, StackProps, Duration, NestedStack, NestedStackProps, CfnOutput, Token, SecretValue} from 'aws-cdk-lib'
import {Construct } from 'constructs';
import { Credentials, DatabaseSecret, DatabaseCluster, DatabaseClusterEngine } from 'aws-cdk-lib/aws-rds';
import {Vpc,SecurityGroup , IVpc, SubnetType, ISecurityGroup, InstanceType, InstanceClass, InstanceSize,Port } from 'aws-cdk-lib/aws-ec2';
import { ParameterDataType,StringParameter } from 'aws-cdk-lib/aws-ssm';
import { Ec2Action } from 'aws-cdk-lib/aws-cloudwatch-actions';
import { CdkResourceInitializer } from '../lib/resource-initializer'
import { DockerImageCode } from "aws-cdk-lib/aws-lambda";
import { RetentionDays } from "aws-cdk-lib/aws-logs";
import {Secret} from 'aws-cdk-lib/aws-secretsmanager';


const bcrypt = require('bcrypt');

export interface RdsConstructProps extends StackProps {
      readonly vpc: Vpc,
      readonly dbConnectionSg:SecurityGroup
    }

    export class RdsConstruct extends Construct {
        readonly vpc:Vpc;
        public readonly dbCluster: DatabaseCluster;
       readonly dbConnectionSg: ISecurityGroup;
       
        constructor(scope:Construct, id : string,  props:RdsConstructProps) {
            super(scope,id)
            this.vpc=props.vpc
            this.dbConnectionSg=props.dbConnectionSg
            const credsSecretName = 'fap-db-secret'
            const prefix = this.node.tryGetContext('PREFIX');
        /**************AURORA*************** */
        const creds = new DatabaseSecret(this, 'MysqlRdsCredentials', {
            secretName:credsSecretName,
            username:'admin'
            }
        )
        
        /*
      this.dbConnectionSg = new ec2.SecurityGroup(this, "DB-securitygroup", {
        description: "File Transfer Admin Portal DB Security Group",
        vpc: this.vpc,
        securityGroupName: "FileTransferAdminPortalDB-SG"
      })
    
      this.dbConnectionSg.addIngressRule(
        ec2.Peer.anyIpv4(),
        ec2.Port.tcp(3306),
        "Allow inbound"
      )*/
    
    
       this.dbCluster = new DatabaseCluster(this, "Database", {
          clusterIdentifier: `${prefix}RdsCluster`,
          engine: DatabaseClusterEngine.AURORA_MYSQL,
          defaultDatabaseName: "FileTransferAdminPortal",
          credentials: Credentials.fromSecret(creds),
          iamAuthentication: true,
          instanceProps: {
            vpc: this.vpc,
            securityGroups: [this.dbConnectionSg],
            
            instanceType: InstanceType.of(InstanceClass.T3, InstanceSize.SMALL),
            vpcSubnets: {
              subnetType: SubnetType.PRIVATE_ISOLATED
            }
          }
      })
    
      new StringParameter(this, "aurora-endpoint", {
        dataType: ParameterDataType.TEXT,
        parameterName: "/Applications/FileTransferAdminPortal/rds_endpoint",
        stringValue: this.dbCluster.clusterEndpoint.hostname
      })
      
      
      
      //Create a strong initial password, store it as a secret, and pass its hash to the sql script.
      var randomPassword = Math.random().toString(36).slice(-8);
      const saltRounds = 12;
      const salt = bcrypt.genSaltSync(saltRounds);
      const pashword = bcrypt.hashSync(randomPassword, salt);
      
      const initialSecretCreds = '{"Username":"admin", "password":"' + randomPassword + '"}'
 
      new Secret(this, 'initialUserSecret', {
        secretName: "FileTransferPortalInitialCreds",
        secretStringValue: SecretValue.unsafePlainText(initialSecretCreds)
      })
      
      const initializer = new CdkResourceInitializer(this, 'MyRdsInit', {
        config: {
          credsSecretName,
          pashword
        },
        
        fnLogRetention: RetentionDays.FIVE_MONTHS,
        fnCode: DockerImageCode.fromImageAsset(`${__dirname}/rds-init-fn-code`, {}),
        fnTimeout: Duration.minutes(2),
        fnSecurityGroups: [],
        vpc: this.vpc,
        subnetsSelection: props.vpc.selectSubnets({
          subnetType: SubnetType.PRIVATE_ISOLATED
        })
      }) 
      // manage resources dependency
      initializer.customResource.node.addDependency(this.dbCluster)
    
      // allow the initializer function to connect to the RDS instance
      this.dbCluster.connections.allowFrom(initializer.function, Port.tcp(3306))
    
      // allow initializer function to read RDS instance creds secret
      creds.grantRead(initializer.function)
      
      /* eslint no-new: 0 */
      new CfnOutput(this, 'RdsInitFnResponse', {
        value: Token.asString(initializer.response)
      }) 
        
        }
    }