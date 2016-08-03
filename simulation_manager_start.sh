#!/bin/bash
./service.sh ScalarmSimulationManager start
pushd $HOME/.cloudify
    bash --login -c "./scalarm_simulation_manager"
popd
