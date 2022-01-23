Jenkins Master Server Deployment according to Arctic requirements via Ansible Playbook
======================================================================================

Adapt the inventory (e.g. environments/dev/hosts) to represent name and IP-address of your machine (Generic Redhat Distribution Installation. Should have an ssh server running).

Change the name of the file inside of the host vars according to your hosts file.

run via: ansible-playbook install_server.yml -k -i environments/dev/

(-k means that you supply the ssh password, -i specifies the inventory to be run against.)

To run only on specific host, add --limit and specify the ansible hostname to be used.

An ansible.cfg is in the same folder and should take precedence over your system-wide settings. Main differences are the changed roles and inventory folder locations.

NOTE: Geerlingguy.jenkins role has been modified to not require openjdk. May need further testing, but this enables us to manually install Oracle Java.


HOW TO RUN MASTER JENKINS INSTALLATION on STAGING environment:
ansible-playbook master.yml -i environments/stage/ArcticTestNet/ --skip-tags "jobs-cloning" --ask-vault-pass -e@vault_stage.yml

HOW TO RUN MASTER JENKINS INSTALLATION on PROD environment:
ansible-playbook master.yml -i environments/prod/ArcticProdNet/ --skip-tags "arctictest-specific-files,jobs-cloning" --ask-vault-pass -e@vault_prod.yml




HOW TO RUN JENKINS SLAVES INSTALLATION on STAGING environment:
On your ansible machine execute the following 2 commands:
eval $(ssh-agent) > /dev/null
ssh-add

...Enter passphrase for the rsa key and you are good to go with the actual execution:
ansible-playbook slaves.yml -i environments/stage/ArcticTestNet/ --skip-tags "graph-package" --ask-vault-pass -e@vault_stage.yml

HOW TO RUN JENKINS SLAVES INSTALLATION on PROD environment:
On your ansible machine execute the following 2 commands:
eval $(ssh-agent) > /dev/null
ssh-add

...Enter passphrase for the rsa key and you are good to go with the actual execution:
ansible-playbook slaves.yml -i environments/prod/ArcticProdNet/ --skip-tags "arctictest-specific-files,graph-package" --ask-vault-pass -e@vault_prod.yml




How to add/update plugins on Jenkins Master on STAGING environment:
ansible-playbook master.yml -i environments/stage/ArcticTestNet/ --tags "jenkins-plugins" --ask-vault-pass -e@vault_stage.yml

How to add/update plugins on Jenkins Master on the PROD environment:
ansible-playbook master.yml -i environments/prod/ArcticProdNet/ --tags "jenkins-plugins" --ask-vault-pass -e@vault_prod.yml
