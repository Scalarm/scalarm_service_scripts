scalarm_service_scripts
=======================

Scripts derivied from Cloudify Scalarm deployment, to control lifecycle of Scalarm services

Use *{install, start, stop, shutdown}.sh scripts.

Requirements: Ubuntu 12.04+, Groovy interpreter

Tested on Ubuntu 14.04.

Attention: the scripts use sudo and they can automatically install applications in your OS!

All servuces should be automatically downloaded and installed by *install scripts.

Order of use:

Installation - fetch services data, install requirements and copy files to ~/.cloudify dir.

+ information_service_install.sh
+ storage_manager_install.sh
+ experiment_manager_install.sh
+ simulation_manager_install.sh

Starting services:

+ information_service_start.sh
+ storage_manager_start.sh
+ experiment_manager_start.sh -> now you can use https://localhost
+ simulation_manager_start.sh -> now experiments will start to compute

Stopping services:

+ simulation_manager_stop.sh
+ experiment_manager_stop.sh
+ storage_manager_stop.sh
+ information_service_stop.sh

Uninstalling services - removing installed files:

+ information_service_shutdown.sh
+ storage_manager_shutdown.sh
+ experiment_manager_shutdown.sh
+ simulation_manager_shutdown.sh




