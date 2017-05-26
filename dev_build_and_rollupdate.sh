#!/bin/bash

source config
export CHE_IMAGE_REPO=${DOCKER_HUB_REGISTRY_PREFIX}${DOCKER_HUB_NAMESPACE}/che-server
export CHE_IMAGE_TAG=nightly-${RH_DIST_SUFFIX}
eval $(minishift docker-env)
bash ./dev_build.sh $*
if [ $? -ne 0 ]; then
  echo 'Build Failed!'
  exit 1
fi

source scripts/setenv-for-deploy.sh
oc rollout latest che -n eclipse-che
