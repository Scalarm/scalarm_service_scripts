def tools = new Tools(args)

def ant = new AntBuilder()

tools.killAllNginxes("nginx-experiment")

def emPublicPort = tools.env['EXPMANPORT_EXTERNAL_PORT']
println "ExperimentManager: my external port is ${emPublicPort}"

tools.deregisterServiceInIS('experiment_managers', "${tools.thisHost}:${emPublicPort}")

tools.commandEnvsByConfig("rake service:stop")
