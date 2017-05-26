#!/bin/bash

if [ $(minishift status) != "Running" ]; then
  echo "The Minishift VM should be running"
  exit 1
fi

commandName = $(dirname "$0")

eval $(minishift docker-env)
bash ${commandName}/dev_build.sh $*
if [ $? -ne 0 ]; then
  echo 'Build Failed!'
  exit 1
fi

source ${commandName}/setenv-for-deploy.sh
oc rollout latest che -n eclipse-che
