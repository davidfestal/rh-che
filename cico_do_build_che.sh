#!/bin/bash

# this script downloads runs the build
# to create the che binaries

. config 

mvnche() {
  which scl 2>/dev/null
  if [ $? -eq 0 ]
  then
    if [ `scl -l 2> /dev/null | grep rh-maven33` != "" ]
    then
      scl enable rh-maven33 rh-nodejs4 "mvn $*"
    else
      mvn $*
    fi
  else
    mvn $*
  fi

}

mkdir $NPM_CONFIG_PREFIX 2>/dev/null
mvnche -B $* install -U
if [ $? -ne 0 ]; then
  echo "Error building che/rh-che with dashboard"
  exit 1;
fi

if [ $DeveloperBuild != "true" ]
  then
    mvnche -B -P'!checkout-base-che' -DwithoutDashboard $* install -U
    if [ $? -ne 0 ]; then
      echo "Error building che/rh-che without dashboard"
      exit 1;
  fi
fi
