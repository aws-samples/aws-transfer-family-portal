// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import {StackProps} from 'aws-cdk-lib'
import {Vpc, SubnetType, FlowLogDestination, SecurityGroup, Port, Peer,InterfaceVpcEndpointAwsService,GatewayVpcEndpointAwsService} from 'aws-cdk-lib/aws-ec2'
import {RemovalPolicy} from 'aws-cdk-lib'
import { Construct } from 'constructs'
import {LogGroup} from 'aws-cdk-lib/aws-logs'
import {Role,ServicePrincipal} from 'aws-cdk-lib/aws-iam'
import {StringParameter} from 'aws-cdk-lib/aws-ssm'
/*******Network********** */

export class NetworkConstruct extends Construct {
    public readonly vpc: Vpc
    constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id);
   
   
    const vpcCidr = this.node.tryGetContext('VPC_CIDR') || '10.194.0.0/21';
    
    //Create a vpc that will host the app
    this.vpc = new Vpc(this, 'Vpc', {
      cidr: vpcCidr,
      natGateways: 1,
      maxAzs: 2,
      enableDnsHostnames: true,
      enableDnsSupport: true,
      subnetConfiguration: [{
        cidrMask: 27,
        name: 'PUBLIC',
        subnetType: SubnetType.PUBLIC,
      }, {
        cidrMask: 27,
        name: 'PRIVATE_ISOLATED',
        subnetType: SubnetType.PRIVATE_ISOLATED,
      }, {
        cidrMask: 27,
        name: 'PRIVATE_WITH_NAT',
        subnetType: SubnetType.PRIVATE_WITH_NAT,
      }],
    });

    const sftpVpcFlowLogsGroup = new LogGroup(this, 'SftpVpcFlowLogsGroup', {
      removalPolicy: RemovalPolicy.DESTROY,
    });

    const sftpVpcFlowLogsRole = new Role(this, 'SftpVpcFlowLogsRole', {
      assumedBy: new ServicePrincipal('vpc-flow-logs.amazonaws.com'),
    });

    this.vpc.addFlowLog('VpcFlowLog', {
      destination: FlowLogDestination.toCloudWatchLogs(
        sftpVpcFlowLogsGroup,
        sftpVpcFlowLogsRole,
      ),
    });

    const endpointsSecurityGroup = new SecurityGroup(this, 'EndpointsSecurityGroup', {
      vpc: this.vpc,
      allowAllOutbound: true,
    });

    endpointsSecurityGroup.applyRemovalPolicy(RemovalPolicy.DESTROY);

    endpointsSecurityGroup.addIngressRule(
      Peer.ipv4(this.vpc.vpcCidrBlock),
      Port.allTraffic(),
    );

    this.vpc.addInterfaceEndpoint('EcrDkrVpcEndpoint', {
      privateDnsEnabled: true,
      securityGroups: [endpointsSecurityGroup],
      service: InterfaceVpcEndpointAwsService.ECR_DOCKER,
      subnets: { subnetType: SubnetType.PRIVATE_ISOLATED, onePerAz: true },
    });

    this.vpc.addInterfaceEndpoint('EcrApiVpcEndpoint', {
      privateDnsEnabled: true,
      securityGroups: [endpointsSecurityGroup],
      service: InterfaceVpcEndpointAwsService.ECR,
      subnets: { subnetType: SubnetType.PRIVATE_ISOLATED, onePerAz: true },
    });

    this.vpc.addInterfaceEndpoint('CloudWatchMonitoringVpcEndpoint', {
      privateDnsEnabled: true,
      securityGroups: [endpointsSecurityGroup],
      service: InterfaceVpcEndpointAwsService.CLOUDWATCH_EVENTS,
      subnets: { subnetType: SubnetType.PRIVATE_ISOLATED, onePerAz: true },
    });

    this.vpc.addInterfaceEndpoint('KmsVpcEndpoint', {
      privateDnsEnabled: true,
      securityGroups: [endpointsSecurityGroup],
      service: InterfaceVpcEndpointAwsService.KMS,
      subnets: { subnetType: SubnetType.PRIVATE_ISOLATED, onePerAz: true },
    });

    this.vpc.addInterfaceEndpoint('SsmVpcEndpoint', {
      privateDnsEnabled: true,
      securityGroups: [endpointsSecurityGroup],
      service: InterfaceVpcEndpointAwsService.SSM,
      subnets: { subnetType: SubnetType.PRIVATE_ISOLATED, onePerAz: true },
    });

    this.vpc.addInterfaceEndpoint('SecretManagerVpcEndpoint', {
      privateDnsEnabled: true,
      securityGroups: [endpointsSecurityGroup],
      service: InterfaceVpcEndpointAwsService.SECRETS_MANAGER,
      subnets: { subnetType: SubnetType.PRIVATE_ISOLATED, onePerAz: true },
    });

    this.vpc.addGatewayEndpoint('S3VpcEndpoint', {
      service: GatewayVpcEndpointAwsService.S3,
      subnets: [{ subnetType: SubnetType.PRIVATE_ISOLATED, onePerAz: true }],
    });

  this.vpc.addInterfaceEndpoint('CloudWatchVpcEndpoint', {
      privateDnsEnabled: true,
      securityGroups: [endpointsSecurityGroup],
      service: InterfaceVpcEndpointAwsService.CLOUDWATCH_LOGS,
      subnets: { subnetType:SubnetType.PRIVATE_ISOLATED, onePerAz: true },
    });

    /* eslint-disable no-new */
    new StringParameter(this, 'CloudWatchVpcEndpointParameter', {
      parameterName: '/Applications/FileTransferAdminPortal/Cloudwatch-VPC-Endpoint',
      stringValue: `https://logs.${props?.env?.region}.amazonaws.com`,
    }).applyRemovalPolicy(RemovalPolicy.DESTROY);
  }


   
    }
