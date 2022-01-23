FROM alpine/git

#TBD: Consider NTP
ARG GIT_USER_EMAIL="jenkins@jenkins.arctic-test-net.alchemy.sofia.ifao.net"
ARG GIT_USER_NAME="Jenkins TEST"
ARG GIT_HTTP_SSL_CA_PATH="/etc/ssl/certs"
ARG GIT_HTTP_SSL_CA_INFO="/etc/ssl/certs/ca-certificates.crt"

COPY ifao-root-ca.crt /etc/ssl/certs/ca-certificates.crt 
RUN chmod 444 /etc/ssl/certs/ca-certificates.crt


RUN git config --system user.name ${GIT_USER_NAME}
RUN git config --system user.email ${GIT_USER_EMAIL}
RUN git config --system http.sslCAPath ${GIT_HTTP_SSL_CA_PATH}
RUN git config --system http.sslcainfo ${GIT_HTTP_SSL_CA_INFO}

RUN git config --global user.name ${GIT_USER_NAME}
RUN git config --global user.email ${GIT_USER_EMAIL}
RUN git config --global http.sslCAPath ${GIT_HTTP_SSL_CA_PATH}
RUN git config --global http.sslcainfo ${GIT_HTTP_SSL_CA_INFO}

