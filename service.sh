#!/bin/bash

cd $1
groovy -cp .. $2.groovy config.properties
