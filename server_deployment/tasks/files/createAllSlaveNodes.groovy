import hudson.model.*
import hudson.slaves.*
import java.util.logging.LogManager
import static groovy.io.FileType.FILES
import jenkins.model.Jenkins
import hudson.EnvVars
import groovy.time.TimeCategory
import groovy.time.TimeDuration
import groovy.json.JsonBuilder
import org.apache.commons.httpclient.*
import org.apache.commons.httpclient.methods.*

Jenkins jenkins = Jenkins.instance
EnvVars envVars = build.getEnvironment(listener);
fileName=envVars.get('WORKSPACE') + "/ansible/slaveProps/";
def folder = new File("${fileName}")
if( !folder.exists() ) {
  folder.mkdirs()
}


folder.eachFileRecurse(FILES) {
    if(it != null && it.name.endsWith('.prop')) {
        it.withReader { r ->
            /* Loading java.util.Properties as defaults makes empty Properties object */
            def props = new Properties()
            props.load(r)
            props.each { key, value ->
                if (value == null) {
                   // #logger("exception on something occurred "+e,e)

                   System.exit(0)
                }
            }

            Slave agent = new DumbSlave(
                "${props.name}",
                "${props.workingFolder}",
                new hudson.slaves.JNLPLauncher())

            agent.nodeDescription = "${props.nodeDescription}"
            if ("${props.numExecutors}".isInteger()) {
                agent.numExecutors = "${props.numExecutors}" as Integer
            }
            else {
                // #logger("exception on something occurred "+e,e)
                System.exit(0)
            }
            agent.labelString = "${props.labelString}"
            if ("${props.mode}" == "NORMAL") {
                agent.mode = Node.Mode.NORMAL
            }
            if ("${props.retentionStrategy}" == "Always") {
                agent.retentionStrategy = new RetentionStrategy.Always()
            }
            // Create a "Permanent Agent"
            Jenkins.instance.addNode(agent)
            if (agent.getComputer().isOffline()) {
              println "agent.labelString:" + agent.labelString
              it << "secret=" + agent.getComputer().getJnlpMac() + System.getProperty("line.separator")       
            }

            println jenkins.model.Jenkins.getInstance().getComputer("${props.name}").getJnlpMac()

            return "Node has been created successfully."
        }
    }
}
