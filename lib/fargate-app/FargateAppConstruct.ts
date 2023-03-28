import {Construct} from 'constructs';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as ec2 from 'aws-cdk-lib/aws-ec2'
import * as iam from 'aws-cdk-lib/aws-iam'
import * as rds from 'aws-cdk-lib/aws-rds'
import * as s3 from 'aws-cdk-lib/aws-s3'
import * as acm from 'aws-cdk-lib/aws-certificatemanager'
import * as ecs from 'aws-cdk-lib/aws-ecs'
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2'
import * as ssm from 'aws-cdk-lib/aws-ssm'
import * as ecr from 'aws-cdk-lib/aws-ecr'
import * as route53targets from 'aws-cdk-lib/aws-route53-targets'
import {Duration, StackProps, Stack, RemovalPolicy, CfnOutput} from 'aws-cdk-lib';


export interface FargateAppStackProps extends StackProps {
  readonly hostedZone?: route53.IHostedZone;
  readonly vpc: ec2.IVpc;
  readonly dbConnectionSg: ec2.ISecurityGroup;
  readonly dbCluster: rds.IDatabaseCluster;
  readonly transferS3Bucket: s3.IBucket;
  readonly transferPublicKeysS3Bucket: s3.IBucket;
    
}

export class FargateAppConstruct extends Construct {
  public readonly loadBalancerARecord: route53.ARecord;
  public readonly albDomainCert?: acm.ICertificate;
  public readonly fargateService: ecs.FargateService;

  constructor(scope: Construct, id: string, props: FargateAppStackProps) {
    super(scope, id);

    const prefix = this.node.tryGetContext('PREFIX');
    const domainName = this.node.tryGetContext('DOMAIN_NAME');
    const certificateArn = this.node.tryGetContext('CERTIFICATE_WILDCARD_ARN');


/****ECR******* */

const ecrRepository = new ecr.Repository(this, "ecr", {
  removalPolicy: RemovalPolicy.DESTROY
})

new ssm.StringParameter(this, "ecr-uri", {
  dataType: ssm.ParameterDataType.TEXT,
  parameterName: "/Applications/FileTransferAdminPortal/webapp-ecr-uri",
  stringValue: ecrRepository.repositoryUri
})


const account = Stack.of(this).account
const region = Stack.of(this).region
const ecrRepoEndpoint = account + ".dkr.ecr." + region +".amazonaws.com"


new ssm.StringParameter(this, "ecr-endpoint", {
  dataType: ssm.ParameterDataType.TEXT,
  parameterName: "/Applications/FileTransferAdminPortal/webapp-ecr-endpoint",
  stringValue: ecrRepoEndpoint
})



    const appTaskExecutionRole = new iam.Role(this, 'AppTaskExecutionRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
    });
    appTaskExecutionRole.addManagedPolicy(iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AmazonECSTaskExecutionRolePolicy'));

    const appTaskRole = new iam.Role(this, 'AppTaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
    });


    props.transferS3Bucket.grantReadWrite(appTaskRole);
    props.transferS3Bucket.grantDelete(appTaskRole);
    props.transferPublicKeysS3Bucket.grantReadWrite(appTaskRole);

    appTaskRole.addManagedPolicy(iam.ManagedPolicy.fromAwsManagedPolicyName('AWSTransferFullAccess'));
    appTaskRole.addManagedPolicy(iam.ManagedPolicy.fromAwsManagedPolicyName('CloudWatchLogsFullAccess'));
    appTaskRole.addManagedPolicy(iam.ManagedPolicy.fromAwsManagedPolicyName('CloudWatchEventsFullAccess'));
    appTaskRole.addManagedPolicy(iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonSESFullAccess'));

    const appTaskRolePolicy = new iam.Policy(this, 'AppTaskRolePolicy', {
      statements: [
        new iam.PolicyStatement({
          effect: iam.Effect.ALLOW,
          resources: ['*'],
          actions: ['secretsmanager:GetSecretValue', 'secretsmanager:RotateSecret'],
        }),
        new iam.PolicyStatement({
          effect: iam.Effect.ALLOW,
          resources: ['*'],
                actions: ['rds-db:connect'],
        }),
        new iam.PolicyStatement({
          effect: iam.Effect.ALLOW,
          resources: ['*'],
          actions: [
            'ssm:Describe*',
            'ssm:Get*',
            'ssm:List*',
          ],
        }),
      ],
    });

    appTaskRolePolicy.attachToRole(appTaskRole);

    const ecsCluster = new ecs.Cluster(this, 'EcsCluster', {
      vpc: props.vpc,
    });

    const fargateServiceSg = new ec2.SecurityGroup(this, 'FargateServiceSg', {
      vpc: props.vpc,
      allowAllOutbound: true,
    });

    fargateServiceSg.applyRemovalPolicy(RemovalPolicy.DESTROY);

   

    const dbImportedSecurityGroupAlbAuth = ec2.SecurityGroup.fromSecurityGroupId(
      this,
      'DbImportedSecurityGroupAlbAuth',
      props.dbConnectionSg.securityGroupId,
      { allowAllOutbound: true, mutable: true },
    );

    dbImportedSecurityGroupAlbAuth.addIngressRule(
      fargateServiceSg,
      ec2.Port.tcp(3306),
    );

    const fargateTaskDefinition = new ecs.FargateTaskDefinition(this, 'FargateTaskDefinition', {
      family: 'FargateTaskDefinition',
      memoryLimitMiB: 7168,
      cpu: 1024,
      executionRole: appTaskExecutionRole,
      taskRole: appTaskRole,
    });

  const containerImage = ecs.ContainerImage.fromEcrRepository(ecrRepository)
     
    const taskContainer = fargateTaskDefinition.addContainer('TaskContainer', {
      image: containerImage,
      logging: ecs.LogDrivers.awsLogs({
        streamPrefix: 'fap-web-app',
        logRetention: 30,
      }),
    
    });
  
    this.fargateService = new ecs.FargateService(this, 'FargateService', {
      cluster: ecsCluster,
      circuitBreaker: {
        rollback: true,
      },
      securityGroups: [fargateServiceSg],
      desiredCount: 0,
      taskDefinition: fargateTaskDefinition,
    });

  
    taskContainer.addPortMappings({ containerPort: 5000 });

    const albSecurityGroup = new ec2.SecurityGroup(this, 'AlbSecurityGroup', {
      vpc: props.vpc,
      allowAllOutbound: true,
    });

    albSecurityGroup.applyRemovalPolicy(RemovalPolicy.DESTROY);

    albSecurityGroup.addIngressRule(
       ec2.Peer.anyIpv4(),
      ec2.Port.allTraffic(),
    );

    const fargateServiceAlb = new elbv2.ApplicationLoadBalancer(this, 'FargateServiceAlb', {
      vpc: props.vpc,
      securityGroup: albSecurityGroup,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC, onePerAz: true },
      internetFacing: true,
    });

    const albAccessLoggingBucket = new s3.Bucket(this, 'AlbAccessLoggingBucket', {
      removalPolicy: RemovalPolicy.DESTROY,
      autoDeleteObjects: true,
    });

    fargateServiceAlb.logAccessLogs(albAccessLoggingBucket, `${prefix.toLowerCase()}-alb-access-logs`);

       const httpsListener = fargateServiceAlb.addListener('HttpsListener', {
        protocol: elbv2.ApplicationProtocol.HTTPS,
        port: 443,
        sslPolicy: elbv2.SslPolicy.FORWARD_SECRECY_TLS12_RES_GCM,
        certificates: [{ certificateArn: certificateArn}],

      });
      
      

      const webTargetGroup = new elbv2.ApplicationTargetGroup(this, 'WebTargetGroup', {
        targetType: elbv2.TargetType.IP,
        port: 5000,
        vpc: props.vpc,
        protocol: elbv2.ApplicationProtocol.HTTP,
        targets: [this.fargateService],
        stickinessCookieDuration: Duration.minutes(30),
      });



        webTargetGroup.configureHealthCheck({
        path: '/login',
        protocol: elbv2.Protocol.HTTP,
        unhealthyThresholdCount: 2,
        timeout: Duration.seconds(30),
        port: '5000',
        interval: Duration.seconds(60),
        healthyThresholdCount: 5,
        healthyHttpCodes: '200,302',
      });

      httpsListener.addTargetGroups('alb-listener-target-group', {
        targetGroups: [webTargetGroup],
      });

      const webListenerAction = elbv2.ListenerAction.forward([webTargetGroup]);

      httpsListener.addAction('WebBalancing', {
        action: webListenerAction,
      });

      const loadBalancerTarget = new route53targets.LoadBalancerTarget(
        fargateServiceAlb,
      );
      const loadBalancerDNS = `filetransferadminportal.${domainName}`;
      
     const myDomainName = this.node.tryGetContext('DOMAIN_NAME');
     const hostedZone = route53.HostedZone.fromLookup(
            this,
            'HostedZone',
            {domainName: myDomainName }
            );
          
      new route53.ARecord(this, 'AlbServerEndpointCnameRecord', {
        zone: hostedZone,
        target: route53.RecordTarget.fromAlias(loadBalancerTarget),
        recordName: loadBalancerDNS,
      });
      new CfnOutput(this, 'UI_URL', { 
        value: 'https://' + loadBalancerDNS + "/login",
        description: 'Web App Endpoint'
        
        
      });
    
  }
}
