#!/bin/bash

COMMAND_DIR=$(dirname "$0")

export nextBuildStep="minishift_build_fabric8_and_deploy.sh"
bash ${COMMAND_DIR}/update_openshift_connector.sh $*
