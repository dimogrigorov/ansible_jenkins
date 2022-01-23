job("createAllSlaveNodes") {
    label('master')
configure {
    it / 'properties' / 'jenkins.model.BuildDiscarderProperty' { // This is the tag of type CONFIGURE
        strategy {
            'daysToKeep'('-1')
            'numToKeep'('1')
            'artifactDaysToKeep'('-1')
            'artifactNumToKeep'('-1')
        }
    }
}
    authorization {
        permissions('admin', [
            'hudson.model.Item.Build',
            'hudson.model.Item.Cancel',
            'hudson.model.Item.Configure',
            'hudson.model.Item.Delete',
            'hudson.model.Item.Discover',
            'hudson.model.Item.Read',
            'hudson.model.Item.Workspace',
            'hudson.model.Run.Delete',
            'hudson.model.Run.Update',
            'hudson.scm.SCM.Tag'
        ])
    }

    wrappers {
        credentialsBinding {
            string('VAULT_PASSWORD', 'VAULT_PASSWORD')
        }
    }

    scm {
        git {
            remote {
                name('SomeName')
                url('https://grigorov@repository.secure.ifao.net:7443/scm/cicd/central-jenkins.git')
                credentials('030a62ae-55f7-4f25-b727-1eed8ccd226a')
            }
            branch('master')
        }
    }

    steps {
        shell '''export VAULT_PASSWORD=${VAULT_PASSWORD}
cd ansible 

ansible-playbook create_vm_from_template.yml \\
--vault-password-file ./vault_pass.py -i inventory  -vvv \\
--extra-vars "@inventory/enforce_value_vars.yml" \\
--extra-vars "host=SlaveVMsGroup \\
              ansible_connection=local"

ansible-playbook generate_props.yml \\
--vault-password-file ./vault_pass.py -i inventory -vvv \\
--extra-vars "@inventory/enforce_value_vars.yml" \\
--extra-vars "host=SlaveVMsGroup \\
              ansible_connection=local"

ansible-playbook main.yml \\
--vault-password-file ./vault_pass.py -i inventory -vvv \\
--extra-vars "@inventory/enforce_value_vars.yml"
'''
        systemGroovyCommand(readFileFromWorkspace('createAllSlaveNodes.groovy')) {
        }

        shell '''export VAULT_PASSWORD=${VAULT_PASSWORD}
export ANSIBLE_HOST_KEY_CHECKING=False

cd ansible/slaveProps

for filename in *.prop; do
        while read -r line || [ -n "$line" ]; do
                LINE_NUM=$((LINE_NUM+1))

                # split line at "=" sign
                IFS="="
                read -r VAR VAL <<< "${line}"

                # delete spaces around the equal sign (using extglob)
                VAR="$(echo -e "${VAR}" | tr -d '[:space:]')"
                VAL="$(echo -e "${VAL}" | tr -d '[:space:]')"

                if [ "$VAR" = "name" ]; then
                        name=$VAL
                fi


        done < $filename
        cd ..
        if [ "$name" != "" ]; then
        	ansible-playbook put_vm_in_state.yml \\
    		--vault-password-file ./vault_pass.py -i inventory -vv \\
    		--extra-vars "state=started \\
        	    	      host=localhost \\
            	   	      name=${name}" \\
    		--extra-vars "@inventory/enforce_value_vars.yml"
	fi
        cd slaveProps
done

for filename in *.prop; do
        while read -r line || [ -n "$line" ]; do
                LINE_NUM=$((LINE_NUM+1))

                # split line at "=" sign
                IFS="="
                read -r VAR VAL <<< "${line}"

                # delete spaces around the equal sign (using extglob)
                VAR="$(echo -e "${VAR}" | tr -d '[:space:]')"
                VAL="$(echo -e "${VAL}" | tr -d '[:space:]')"

                if [ "$VAR" = "name" ]; then
                        name=$VAL
                fi

                if [ "$VAR" = "secret" ]; then
                        secret=$VAL
                fi
                
                if [ "$VAR" = "labelString" ]; then
                        label=$VAL
                fi


        done < $filename
        cd ..
        if [ "$name" != "" ] && [ "$secret" != "" ] && [ "$label" != "" ]; then
   		ansible-playbook wait_for_machine_port_to_become_available.yml \\
    		--vault-password-file ./vault_pass.py -i inventory -vv \\
    		--extra-vars "timeout=130 \\
                              port_to_wait_for=22 \\
                  	      ip_address={{ hostvars['${name}'].ansible_host }}" \\
    		--extra-vars "@inventory/enforce_value_vars.yml" 
    
    
    		ansible-playbook initiate_agent_connection.yml \\
    		--vault-password-file ./vault_pass.py -i inventory -vvv \\
    		--extra-vars "host=${name} \\
          	              slave_secret=${secret} \\
                              label=${label}" \\
    		--extra-vars "@inventory/enforce_value_vars.yml"

        	cd slaveProps
        fi
done
        '''

    }
}


job("createAllTeamsMultiBranchScanJobs") {
    label('master')
    authorization {
        permissions('admin', [
            'hudson.model.Item.Build',
            'hudson.model.Item.Cancel',
            'hudson.model.Item.Configure',
            'hudson.model.Item.Delete',
            'hudson.model.Item.Discover',
            'hudson.model.Item.Read',
            'hudson.model.Item.Workspace',
            'hudson.model.Run.Delete',
            'hudson.model.Run.Update',
            'hudson.scm.SCM.Tag'
        ])
    }

    scm {
        git {
            remote {
                name('SomeName')
                url('https://grigorov@repository.secure.ifao.net:7443/scm/cicd/central-jenkins.git')
                credentials('030a62ae-55f7-4f25-b727-1eed8ccd226a')
            }
            branch('master')
        }
    }

    steps {
        dsl(['createMultibranchJobsForAllTeams.groovy'], 'IGNORE')
    }
}
