// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
import {aws_ec2 as ec2, aws_lambda as lambda, aws_route53 as route53, 
    aws_certificatemanager as acm, aws_transfer as transfer, aws_iam as iam, aws_ssm as ssm} from "aws-cdk-lib";
import {StackProps, Duration, Tags, RemovalPolicy} from "aws-cdk-lib";
import {Construct} from 'constructs';

export interface TransferServerStackProps extends StackProps {
    readonly vpc: ec2.IVpc;
    readonly functionArn: string;
    readonly domainName?: string;
    readonly hostedZone?: route53.IHostedZone;
  }
  
  export class TransferServerConstruct extends Construct{ 
    public readonly transferServer: transfer.CfnServer;
    public readonly transferServerEndpointCnameRecord: route53.CnameRecord;
    public readonly transferDomainCert?: acm.ICertificate;
  
    constructor(scope: Construct, id: string, props: TransferServerStackProps) {
      super(scope, id);
      
      const prefix = this.node.tryGetContext('PREFIX');
      const transferSubdomain = this.node.tryGetContext('TRANSFER_SUBDOMAIN');
      const certificateArn = this.node.tryGetContext('CERTIFICATE_WILDCARD_ARN');
      const customHostnameCheck = props.hostedZone && props.domainName && transferSubdomain;
      
      if (customHostnameCheck) {
        if (certificateArn) {
          this.transferDomainCert = acm.Certificate.fromCertificateArn(this, 'TransferDomainCert', certificateArn);
        } else {
          this.transferDomainCert = new acm.DnsValidatedCertificate(this, 'TransferDomainCert', {
            domainName: `${transferSubdomain}.${props.domainName}`,
            hostedZone: props.hostedZone!,
            cleanupRoute53Records: true,
          });
        }
      }
      
      /*Security group for the Transfer Family instance.  It is meant to be public facing and
        allows standard SFTP ports.    
      */
      const transferServerSg = new ec2.SecurityGroup(this, 'TransferServerSg', {
        vpc: props.vpc,
        allowAllOutbound: true,
      });
      
      transferServerSg.applyRemovalPolicy(RemovalPolicy.DESTROY);
  
        transferServerSg.addIngressRule(
          ec2.Peer.anyIpv4(),
          ec2.Port.tcp(22),
        );
        transferServerSg.addIngressRule(
          ec2.Peer.anyIpv4(),
          ec2.Port.tcp(21),
        );
        transferServerSg.addIngressRule(
          ec2.Peer.anyIpv4(),
          ec2.Port.tcpRange(8192, 8200),
        );
    
      const transferServerLoggingRole = new iam.Role(this, 'TransferServerLoggingRole', {
        assumedBy: new iam.ServicePrincipal('transfer.amazonaws.com'),
      });
      transferServerLoggingRole.addManagedPolicy(iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSTransferLoggingAccess'));
      
      // Note: The number of Elastic IP addresses must match the number of
      // Availability Zones that you use with your server endpoints.
      
      const EIPs = new Array<ec2.CfnEIP>(2);
  
      for (let i = 0; i < EIPs.length; i += 1) {
        EIPs[i] = new ec2.CfnEIP(this, `${prefix}EIP${i}`, {
          domain: 'vpc',
        });
      }
      
      this.transferServer = new transfer.CfnServer(this, 'TransferServer', {
        domain: 'S3',
        certificate: this.transferDomainCert?.certificateArn,
        protocols: (this.transferDomainCert ? ['SFTP', 'FTPS'] : ['SFTP']),
        loggingRole: transferServerLoggingRole.roleArn,
        endpointType: 'VPC',
        endpointDetails: {
          subnetIds: props.vpc.publicSubnets.map((s) => s.subnetId),
          vpcId: props.vpc.vpcId,
          securityGroupIds: [transferServerSg.securityGroupId],
          addressAllocationIds: EIPs.map((s) => s.attrAllocationId),
        },
        identityProviderType: 'AWS_LAMBDA',
        identityProviderDetails: {
          //function: props.transferAuthFn.functionArn,
          function: props.functionArn
        },
        securityPolicyName: 'TransferSecurityPolicy-FIPS-2020-06',
     
      });
      
      
      
      
      
      
      const transferEndpoint = `${this.transferServer.attrServerId}.server.transfer.${props.env?.region}.amazonaws.com`;
      let customHostname = transferEndpoint;
      
      
      
      if (customHostnameCheck) {
        customHostname = `${transferSubdomain}.${props.domainName}`;
        Tags.of(this.transferServer).add('aws:transfer:route53HostedZoneId', `/hostedzone/${props.hostedZone!.hostedZoneId}`);
        Tags.of(this.transferServer).add('aws:transfer:customHostname', customHostname);
        this.transferServerEndpointCnameRecord = new route53.CnameRecord(this, 'TransferServerEndpointCnameRecord', {
          domainName: transferEndpoint,
          zone: props.hostedZone!,
          comment: 'TransferServerEndpointCnameRecord',
          recordName: transferSubdomain,
          ttl: Duration.minutes(30),
        });
        new ssm.StringParameter(this, 'CustomHostnameParameter', {
          parameterName: '/Applications/FileTransferAdminPortal/Custom-Hostname',
          stringValue: customHostname,
        });
      }
  
      new ssm.StringParameter(this, 'SftpEndpointParameter', {
        parameterName: '/Applications/FileTransferAdminPortal/SFTP-Endpoint',
        stringValue: transferEndpoint,
      });
  
      new ssm.StringParameter(this, 'SFTPServerIdParameter', {
        parameterName: '/Applications/FileTransferAdminPortal/SFTP-Server-Id',
        stringValue: this.transferServer.attrServerId,
      });
      new ssm.StringParameter(this, 'TransferLogGroupNameParameter', {
        parameterName: '/Applications/FileTransferAdminPortal/TransferLogGroupName',
        stringValue: `/aws/transfer/${this.transferServer.attrServerId}`,
      });
      
      
      
      
      
    }
  }
  