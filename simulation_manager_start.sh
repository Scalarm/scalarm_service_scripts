#!/bin/bash
./service.sh ScalarmSimulationManager start
pushd $HOME/.cloudify/scalarm_simulation_manager
    bash --login -c "./scalarm_simulation_manager"
popd