#!/bin/sh

function parse_inventory_and_get_ip_by_given_vmid {
local VMID=-1
local INVENTORY_FILE=""
local IP=""
local VALID_VMID_SECTION=false


while [[ $# > 0 ]]; do
  key="$1"
  case $key in
    -f)
      INVENTORY_FILE=$2
      shift
    ;;

    -v)
      VMID=$2
      shift
    ;;

    -h|--help|-?)
      echo "$0 [options]"
      echo " -v <vmid> Vmid marker"
      echo " -f <file> Inventory file to parse"
      exit 0
    ;;


    *)
      # unknown option
      echo "Error: unknown option $1"
      exit 1
    ;;

  esac
  shift # past argument or value
done


while read -r line || [ -n "$line" ]; do
	for word in $line; do
        	if [[ "${word}" =~ ^.*=.{1,} ]]; then
			IFS='=' read -r VAR VAL <<< "$word"
	                # delete spaces around the equal sign (using extglob)
	                VAR="$(echo -e "${VAR}" | tr -d '[:space:]')"
        	        VAL="$(echo -e "${VAL}" | tr -d '[:space:]')"

			if [ "$VAR" = "vmid" ] || [ "$VAR" = "ansible_host" ]; then
                      		if [[ "$VAL" =~ ^\".*\"$  ]]; then
                       	        # remove existing double quotes
                	                VAL="${VAL%\"}"
        	                        VAL="${VAL#\"}"
	                        elif [[ "$VAL" =~ ^\'.*\'$ ]]; then
                                	# remove existing single quotes
                        	         VAL="${VAL#\'}"
                	                 VAL="${VAL%\'}"
        	                fi
	                fi


        		if [ "$VAR" = "vmid" ] && [ $VAL -eq $VMID ]; then
	  	        	VALID_VMID_SECTION=true
			        if [ "$IP" != "" ]; then
					echo $IP
					break
			        fi    
	                fi
	                if [ "$VAR" = "ansible_host" ]; then
	        	        if [ "$VALID_VMID_SECTION" = true ]; then
	                	        echo $VAL
	                                break
	                        else
	                        	IP=$VAL
	                        fi
	               fi
		fi
        done
	IP=""
	if [ "$VALID_VMID_SECTION" = true ]; then
		break
	fi
done < $INVENTORY_FILE

}



SERVER=alchemy4
USERNAME=root@pam
NODE=alchemy4
PASSWORD=******
FROM_VMID=6000
TO_VMID=6006
SUBNET_NAME="Testnet"
SUBNET_MASK=255.255.255.240
INVENTORY_DIR="some-path/inventory/Testnet/"

while [[ $# > 0 ]]; do
  key="$1"
  case $key in
    -u)
      USERNAME=$2
      shift
    ;;

    -p)
      PASSWORD=$2
      shift
    ;;

    -f)
      FROM_VMID=$2
      shift
    ;;
    
    -t)
      TO_VMID=$2
      shift
    ;;
    
    -s)  
      SERVER=$2
      shift
    ;;

    -m)
      SUBNET_MASK=$2
      shift
    ;;

    -i)
      INVENTORY_DIR=$2
      shift
    ;;

    -d)
      PLAYBOOK_DIR=$2
      shift
    ;;

    -h|--help|-?)
      echo "Usage:"
      echo "$0 [options]"
      echo " -u <username>       Username, default $USERNAME"
      echo " -p <password>       Password, default $PASSWORD"
      echo " -s <server>         Server to connect to, default $SERVER"
      echo " -f <fromVmid>       First available subnet vmid number, default $FROM_VMID"
      echo " -t <toVmid>         Last available subnet vmid number, default $TO_VMID"
      echo " -m <subnetMask>     Internal subnet-mask, default $SUBNET_MASK"
      echo " -i <inventoryDir>   The main inventory directory passed, default $INVENTORY_DIR"
      echo " -d <playbookDir>    The main  directory passed, default $PLAYBOOK_DIR"      

      exit 0
    ;;


    *)
      # unknown option
      echo "Error: unknown option $1"
      exit 1
    ;;

  esac
  shift # past argument or value
done


RESPONSE=$(curl -s -k -d "username=$USERNAME&password=$PASSWORD" https://$SERVER:8006/api2/json/access/ticket)
TOKEN=$(echo $RESPONSE | jq -r .data.ticket)
NODES=$(curl -s -k https://$SERVER:8006/api2/json/nodes -b "PVEAuthCookie=$TOKEN" | jq -r '.data[].node')

for NODE in $(echo $NODES); do
	curl -s -k https://$SERVER:8006/api2/json/nodes/$NODE/qemu -b "PVEAuthCookie=$TOKEN" > ~/tmp/proxvm-qemu.json
  	for VMID in $(cat ~/tmp/proxvm-qemu.json | jq -r .data[].vmid); do
    		if [ $VMID -le $TO_VMID ] && [ $VMID -ge $FROM_VMID ]; then
      			curl -s -k https://$SERVER:8006/api2/json/nodes/$NODE/qemu/$VMID/config -b "PVEAuthCookie=$TOKEN" > ~/tmp/proxvm-$VMID.json
      			JSON=$(cat ~/tmp/proxvm-qemu.json | jq -r ".data[] | select(.vmid | tonumber | contains($VMID))") 
      			NET=$(cat ~/tmp/proxvm-$VMID.json | jq -r .data.net0)
      			HWADDR=$(echo $NET | sed -re "s/[a-zA-Z0-9]+=([a-zA-Z0-9:]+),.*/\1/g")
      			IP=$(parse_inventory_and_get_ip_by_given_vmid -v $VMID -f $INVENTORY_DIR/slave_vms)
      			if [[ "$IP" != "" ]]; then
				echo "Creating dhcp entry with following values: MASK=$SUBNET_MASK MAC=$HWADDR IP=$IP"
        			HWADDR=$(echo $HWADDR|sed 's/\(.*\)/\L\1/')
        			HWADDR=$(echo $HWADDR|sed 's/://g')
        			HWADDR=$(echo $HWADDR|sed 's/.\{4\}/&-/g')
        			HWADDR=$(echo $HWADDR|sed 's/.$//')
      				sed -i "/forbidden-ip/a\ static-bind ip-address $IP mask $SUBNET_MASK hardware-address $HWADDR" $PLAYBOOK_DIR/files/template.cfg
			fi  
    		fi
  	done
done
