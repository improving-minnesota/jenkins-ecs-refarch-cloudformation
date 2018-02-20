FROM jenkins/jenkins:lts-alpine
MAINTAINER Chris W. Gerber <christopher.gerber@objectpartners.com>


ENV JENKINS_HTTP_PORT 8080
ENV JENKINS_OPTS="--httpPort=$JENKINS_HTTP_PORT"
ENV JENKINS_AGENT_IMAGE jenkinsci/jnlp-slave
ENV JENKINS_SLAVE_IAM_ROLE_ARN ''
ENV ECS_CLUSTER_ARN ''
ENV AWS_REGION 'us-east-1'


COPY ref/ /usr/share/jenkins/ref/


USER root

RUN \
  apk -Uuv add groff less python py-pip && \
  pip install awscli && \
  apk --purge -v del py-pip && \
  rm /var/cache/apk/* && \
  chown -R jenkins:jenkins /usr/share/jenkins/ref/

USER jenkins


RUN \
  echo 2.0 > /usr/share/jenkins/ref/jenkins.install.UpgradeWizard.state && \
  /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt
