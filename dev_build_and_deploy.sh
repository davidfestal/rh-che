#!/bin/bash

if [ $(minishift status) != "Running" ]; then
  echo "The Minishift VM should be running"
  exit 1
fi

source config
export CHE_IMAGE_REPO=${DOCKER_HUB_NAMESPACE}/che-server
export CHE_IMAGE_TAG=nightly-${RH_DIST_SUFFIX}
eval $(minishift docker-env)
bash ./dev_build.sh $*
if [ $? -ne 0 ]; then
  echo 'Build Failed!'
  exit 1
fi

source scripts/setenv-for-deploy.sh
bash scripts/delete-all.sh
bash scripts/create-all.sh
