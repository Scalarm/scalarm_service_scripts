import os, stat

def create(camel, snake, action):
    content='''#!/bin/bash
./service.sh %s %s
''' % (camel, action)

    fname = '%s_%s.sh' % (snake, action)
    with open(fname, 'wb+') as f:
        f.write(content)
    os.system('chmod +x ' + fname)
    
names = [('ScalarmInformationService', 'information_service'),
         ('ScalarmExperimentManager', 'experiment_manager'),
         ('ScalarmStorageManager', 'storage_manager'),
         ('ScalarmSimulationManager', 'simulation_manager')]

for camel, snake in names:
    for action in ['install', 'start', 'stop', 'shutdown']:
        create(camel, snake, action)
