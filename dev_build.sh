#!/bin/bash

export BuildUser=$USER
export DeveloperBuild="true"
bash ./cico_build.sh $*
