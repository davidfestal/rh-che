#!/bin/bash

export BuildUser=$USER
export DeveloperBuild="true"
bash ./cico_build.sh $*
if [ $? -ne 0 ]; then
  echo 'Build Failed!'
  exit 1
fi
