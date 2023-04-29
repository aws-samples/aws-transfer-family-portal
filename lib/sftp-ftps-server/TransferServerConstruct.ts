// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import {StackProps, Duration, Tags, RemovalPolicy} from "aws-cdk-lib";
import {IVpc, SecurityGroup, Peer,Port,CfnEIP} from 'aws-cdk-lib/aws-ec2';
import {IHostedZone, CnameRecord} from 'aws-cdk-lib/aws-route53';
import {CfnServer} from 'aws-cdk-lib/aws-transfer';
import {Construct} from 'constructs';
import {Certificate, ICertificate, DnsValidatedCertificate} from 'aws-cdk-lib/aws-certificatemanager';
import {ServicePrincipal, ManagedPolicy, Role} from 'aws-cdk-lib/aws-iam';
import {StringParameter} from 'aws-cdk-lib/aws-ssm';


export interface TransferServerStackProps extends StackProps {
    readonly vpc: IVpc;
    readonly functionArn: string;
    readonly domainName?: string;
    readonly hostedZone?: IHostedZone;
  }
  
  export class TransferServerConstruct extends Construct{ 
    public readonly transferServer: CfnServer;
    public readonly transferServerEndpointCnameRecord:CnameRecord;
    public readonly transferDomainCert?: ICertificate;
  
    constructor(scope: Construct, id: string, props: TransferServerStackProps) {
      super(scope, id);
      
      const prefix = this.node.tryGetContext('PREFIX');
      const transferSubdomain = this.node.tryGetContext('TRANSFER_SUBDOMAIN');
      const certificateArn = this.node.tryGetContext('CERTIFICATE_WILDCARD_ARN');
      const customHostnameCheck = props.hostedZone && props.domainName && transferSubdomain;
      
      if (customHostnameCheck) {
        if (certificateArn) {
          this.transferDomainCert = Certificate.fromCertificateArn(this, 'TransferDomainCert', certificateArn);
        } else {
          this.transferDomainCert = new DnsValidatedCertificate(this, 'TransferDomainCert', {
            domainName: `${transferSubdomain}.${props.domainName}`,
            hostedZone: props.hostedZone!,
            cleanupRoute53Records: true,
          });
        }
      }
      
      /*Security group for the Transfer Family instance.  It is meant to be public facing and
        allows standard SFTP ports.    
      */
      const transferServerSg = new SecurityGroup(this, 'TransferServerSg', {
        vpc: props.vpc,
        allowAllOutbound: true,
      });
      
      transferServerSg.applyRemovalPolicy(RemovalPolicy.DESTROY);
  
        transferServerSg.addIngressRule(
          Peer.anyIpv4(),
          Port.tcp(22),
        );
        transferServerSg.addIngressRule(
          Peer.anyIpv4(),
          Port.tcp(21),
        );
        transferServerSg.addIngressRule(
          Peer.anyIpv4(),
          Port.tcpRange(8192, 8200),
        );
    
      const transferServerLoggingRole = new Role(this, 'TransferServerLoggingRole', {
        assumedBy: new ServicePrincipal('transfer.amazonaws.com'),
      });
      transferServerLoggingRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSTransferLoggingAccess'));
      
      // Note: The number of Elastic IP addresses must match the number of
      // Availability Zones that you use with your server endpoints.
      
      const EIPs = new Array<CfnEIP>(2);
  
      for (let i = 0; i < EIPs.length; i += 1) {
        EIPs[i] = new CfnEIP(this, `${prefix}EIP${i}`, {
          domain: 'vpc',
        });
      }
      
      this.transferServer = new CfnServer(this, 'TransferServer', {
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
        this.transferServerEndpointCnameRecord = new CnameRecord(this, 'TransferServerEndpointCnameRecord', {
          domainName: transferEndpoint,
          zone: props.hostedZone!,
          comment: 'TransferServerEndpointCnameRecord',
          recordName: transferSubdomain,
          ttl: Duration.minutes(30),
        });
        new StringParameter(this, 'CustomHostnameParameter', {
          parameterName: '/Applications/FileTransferAdminPortal/Custom-Hostname',
          stringValue: customHostname,
        });
      }
  
      new StringParameter(this, 'SftpEndpointParameter', {
        parameterName: '/Applications/FileTransferAdminPortal/SFTP-Endpoint',
        stringValue: transferEndpoint,
      });
  
      new StringParameter(this, 'SFTPServerIdParameter', {
        parameterName: '/Applications/FileTransferAdminPortal/SFTP-Server-Id',
        stringValue: this.transferServer.attrServerId,
      });
      new StringParameter(this, 'TransferLogGroupNameParameter', {
        parameterName: '/Applications/FileTransferAdminPortal/TransferLogGroupName',
        stringValue: `/aws/transfer/${this.transferServer.attrServerId}`,
      });
  }
  }
  