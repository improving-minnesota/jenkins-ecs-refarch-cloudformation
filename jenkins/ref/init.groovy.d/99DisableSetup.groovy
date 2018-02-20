#!groovy
import jenkins.model.*
import hudson.security.*
import jenkins.install.InstallState
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;

// Look up Jenkins Username and Password stored in SSM and import them into Jenkins.

def env = System.getenv()
def adminUsernameKey = env.JENKINS_ADMIN_USERNAME_KEY
def adminPasswordKey = env.JENKINS_ADMIN_PASSWORD_KEY

def jenkins = Jenkins.getInstance()
jenkins.setSecurityRealm(new HudsonPrivateSecurityRealm(false))
jenkins.setAuthorizationStrategy(new GlobalMatrixAuthorizationStrategy())

def ssm = AWSSimpleSystemsManagementClientBuilder.defaultClient();

// Username
GetParameterRequest usernameParameterRequest = new GetParameterRequest();
usernameParameterRequest.withName(adminUsernameKey).setWithDecryption(Boolean.valueOf(true));
GetParameterResult usernameParameterResult = ssm.getParameter(usernameParameterRequest);
Parameter usernameParameter = usernameParameterResult.getParameter();

// Password
GetParameterRequest passwordParameterRequest = new GetParameterRequest();
passwordParameterRequest.withName(adminPasswordKey).setWithDecryption(Boolean.valueOf(true));
GetParameterResult passParameterResult = ssm.getParameter(passwordParameterRequest);
Parameter passwordParameter = passParameterResult.getParameter();

def user = jenkins.getSecurityRealm().createAccount(usernameParameter.getValue(), passwordParameter.getValue())
user.save()

jenkins.getAuthorizationStrategy().add(Jenkins.ADMINISTER, usernameParameter.getValue())
jenkins.save()
