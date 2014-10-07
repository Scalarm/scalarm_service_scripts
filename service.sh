#!/bin/bash
LOCAL_IP=$3
if [ -z "$LOCAL_IP" ]; then
    LOCAL_IP=localhost
fi

IS_IP=$4
if [ -z "$IS_IP" ]; then
    IS_IP=localhost
fi

cd $1
groovy -cp .. $2.groovy config.properties $LOCAL_IP $IS_IP
