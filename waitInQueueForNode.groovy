import hudson.model.*
import groovy.time.TimeCategory
import groovy.time.TimeDuration
import groovy.json.JsonBuilder
import org.apache.commons.httpclient.*
import org.apache.commons.httpclient.methods.*
import jenkins.model.Jenkins
import hudson.EnvVars


Jenkins jenkins = Jenkins.instance
def jenkinsNodes =jenkins.nodes


def queues = Hudson.instance.queue
println "Queue contains ${queues.items.length} items"

// get the queue list from jenkins instance , [name: waitingminutes]

start = new Date()

def longQueues = [:]
for(queue in queues.items) {
    println(queue.getInQueueSince())
    println(new Date(((long)queue.getInQueueSince())))
    println(queue.task.name)
    TimeDuration td = TimeCategory.minus( start,new Date(((long)queue.getInQueueSince())) )
    longQueues[queue.task.name] = td.getMinutes() + 1
    println(Hudson.instance.getJob(queue.task.name).assignedLabel)
    for (Node node in jenkinsNodes)  {
        // Check if slave is offline
        if (node.getComputer().isOffline()) {
            println "node.labelString:" + node.labelString
            println "Hudson.instance.getJob(queue.task.name).assignedLabel:" + Hudson.instance.getJob(queue.task.name).assignedLabel
            if("${node.labelString}"=="${Hudson.instance.getJob(queue.task.name).assignedLabel}") {
                EnvVars envVars = build.getEnvironment(listener)
                fileName=envVars.get('WORKSPACE') + "/jnlpSecret.txt"
                def jnlpSecret = new File(fileName)
                jnlpSecret.write node.name + System.getProperty("line.separator")
                jnlpSecret << node.getComputer().getJnlpMac() + System.getProperty("line.separator")       
                return;
             }
        }
   }
   println "no available slaves with desired label"
}
