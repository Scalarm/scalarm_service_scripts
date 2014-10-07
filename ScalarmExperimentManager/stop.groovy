def tools = new Tools(args)

def ant = new AntBuilder()

tools.killAllNginxes("nginx-experiment")

tools.deregisterExperimentManager()

tools.commandProduction("rake service:stop")

tools.optionalCommandProduction("rake db_router:stop")