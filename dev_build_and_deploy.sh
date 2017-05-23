#!/bin/bash

source config
export CHE_IMAGE_REPO=${DOCKER_HUB_REGISTRY_PREFIX}${DOCKER_HUB_NAMESPACE}/che-server
export CHE_IMAGE_TAG=nightly-${RH_DIST_SUFFIX}
eval $(minishift docker-env)
bash ./dev_build.sh $*
source scripts/setenv-for-deploy.sh
bash scripts/delete-all.sh
bash scripts/create-all.sh