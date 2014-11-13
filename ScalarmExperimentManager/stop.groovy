def tools = new Tools(args)

def ant = new AntBuilder()

tools.killAllNginxes("nginx-experiment")

tools.deregisterExperimentManager()

tools.commandEnvsByConfig("rake service:stop")

tools.optionalCommandEnvsByConfig("rake db_router:stop")