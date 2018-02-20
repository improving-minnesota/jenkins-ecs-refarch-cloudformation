import java.util.logging.Logger
import jenkins.model.*
import hudson.model.Node.Mode
import hudson.model.UsageStatistics

Jenkins jenkins = Jenkins.getInstance()
Logger logger = Logger.getLogger("")

def jenkinsMode = Mode.EXCLUSIVE
def sysMessage = """\
  Your Jenkins Server.
  """.stripIndent()

hudson.model.UsageStatistics.DISABLED = true
jenkins.setSystemMessage(sysMessage)
jenkins.setNumExecutors(0)
jenkins.setMode(jenkinsMode)
jenkins.setSlaveAgentPort([50000])

Set<String> agentProtocolsList = ['JNLP4-connect', 'Ping']
if(!jenkins.getAgentProtocols().equals(agentProtocolsList)) {
    jenkins.setAgentProtocols(agentProtocolsList)
    println "Agent Protocols have changed.  Setting: ${agentProtocolsList}"
} else {
    println "Nothing changed.  Agent Protocols already configured: ${jenkins.getAgentProtocols()}"
}

jenkins.save()
logger.info("Finished configuring the system message.")
