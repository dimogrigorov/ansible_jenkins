FROM jenkins/slave:3.27-1
COPY ifao-root-ca.crt /home/jenkins/ifao-root-ca.crt
USER root
RUN keytool -importcert -file /home/jenkins/ifao-root-ca.crt -alias ifao-root-ca -keystore /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/cacerts -storepass "changeit" -noprompt
RUN rm -f ~/ifao-root-ca.crt

COPY jenkins-slave /usr/local/bin/jenkins-slave

ENTRYPOINT ["jenkins-slave"]
