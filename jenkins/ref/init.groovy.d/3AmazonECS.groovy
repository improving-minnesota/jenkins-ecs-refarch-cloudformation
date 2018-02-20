#!groovy

import java.util.Arrays
import java.util.logging.Logger
import jenkins.model.*
import com.cloudbees.jenkins.plugins.amazonecs.ECSTaskTemplate
import com.cloudbees.jenkins.plugins.amazonecs.ECSTaskTemplate.MountPointEntry
import com.cloudbees.jenkins.plugins.amazonecs.ECSTaskTemplate.EnvironmentEntry
import com.cloudbees.jenkins.plugins.amazonecs.ECSCloud

println '--> starting ecs config'
instance = Jenkins.getInstance()

println '--> creating mounts'
def mounts = Arrays.asList(
    // Uncomment to add Docker socket to the agent.
    // new MountPointEntry(
    //     name="docker",
    //     sourcePath="/var/run/docker.sock",
    //     containerPath="/var/run/docker.sock",
    //     readOnly=false),
    new MountPointEntry(
        name="jenkins-tools",
        sourcePath="/var/jenkins_home/tools",
        containerPath="/home/jenkins/tools",
        readOnly=false),
)

println '--> creating templates'
def ecsTemplate = new ECSTaskTemplate(
    templateName="jnlp-agent",
    label="any",
    image=System.getenv('JENKINS_AGENT_IMAGE'),
    remoteFSRoot="/home/jenkins",
    memory=0,
    memoryReservation=512,
    cpu=256,
    privileged=true,
    logDriverOptions=null,
    environments=null,
    extraHosts=null,
    mountPoints=mounts
)
ecsTemplate.setTaskrole(System.getenv('JENKINS_AGENT_IAM_ROLE_ARN'))

println '--> creating ECS cloud'
ecsCloud = new ECSCloud(
  name="ecs",
  templates=Arrays.asList(ecsTemplate),
  credentialsId=null,
  cluster=System.getenv('ECS_CLUSTER_ARN'),
  regionName=System.getenv('AWS_REGION'),
  jenkinsUrl=null,
  slaveTimoutInSeconds=60
)

def clouds = instance.clouds
println '--> adding cloud'
clouds.add(ecsCloud)
println '--> saving'
instance.save()
