def tools = new Tools(this.args)

tools.waitForInformationService()

def nginxConfigDir = "${tools.installDir}/nginx-storage"

// generate config file (we are sure that we know thisHost)
// mongo_host - assume that mongo is running within this instance and listens on thisHost private port
//
def scalarmYML = """\
# log_bank - assume mongo is on current host listening on Docker interface
mongo_host: ${tools.thisHostDocker}
mongo_port: 27017
db_name: 'scalarm_db'
binaries_collection_name: 'simulation_files'

# mongodb - bind to thisHostDocker; also local LogBank will use it
host: ${tools.thisHostDocker}
auth_username: ${tools.config.mongoUsername}
auth_password: ${tools.config.mongoPassword}

## distributed db_instance and db_config_service are not used in this simple installation
#db_instance_port: 30000
## db_instance paths are used by start_single
db_instance_dbpath: ./../../scalarm_db_data
db_instance_logpath: ./../../log/scalarm_db.log
#db_config_port: 28000
#db_config_dbpath: ./../../scalarm_db_config_data
#db_config_logpath: ./../../log/scalarm_db_config.log
## db_router_host - where db_router listens - the same as host in this installation
#db_router_host: ${tools.thisHost}
db_router_port: 27017
db_router_logpath: ./../../log/scalarm_db_router.log
monitoring:
  db_name: 'scalarm_monitoring'
  metrics: 'cpu'
  interval: 60
"""

new File("${tools.serviceConfigDir}/scalarm.yml").withWriter { out ->
    out.writeLine(scalarmYML)
}

// in case if one of service parts is still running after some failure
tools.optionalCommand('rake service:stop_single', tools.serviceDir, [
    'RAILS_ENV': 'production',
    'IS_URL': "${tools.getIsHost()}:${tools.isPort}",
    'IS_USER': tools.config.isUser,
    'IS_PASS': tools.config.isPass
])

// initialize db_instance with password auth
tools.command('rake db_instance:create_auth', tools.serviceDir, [
    'RAILS_ENV': 'production',
    'IS_URL': "${tools.getIsHost()}:${tools.isPort}",
    'IS_USER': tools.config.isUser,
    'IS_PASS': tools.config.isPass
])


// manually start mongodb because then we can register it in IS manually
// TODO: this should be taken from config
def db_data_dir = "./../../scalarm_db_data"
new AntBuilder().mkdir(dir: "mongodb/bin/${db_data_dir}")

// TODO: this should be taken from config
def mongo_log_path = "./../../log/scalarm_db.log"

tools.command("./mongod --bind_ip 0.0.0.0 --port 27017 --dbpath ${db_data_dir} --logpath ${mongo_log_path} --rest --httpinterface --fork --smallfiles --auth", "${tools.serviceDir}/mongodb/bin")

// TODO: get mongo router public port
def mongodbPublicPort = tools.env['PUBLIC_MONGODBPORT']
println "StorageManager MongoDB: my external port is ${mongodbPublicPort}"

tools.registerServiceInIS("db_routers", "${tools.thisHost}:${mongodbPublicPort}")

// start single mongo instance (mongod) and log_bank
// will listen on db_router_port and
tools.command('rake log_bank:start', tools.serviceDir, [
    'RAILS_ENV': 'production',
    'IS_URL': "${tools.getIsHost()}:${tools.isPort}",
    'IS_USER': tools.config.isUser,
    'IS_PASS': tools.config.isPass
])

// Kill found nginx-storage processes
tools.killAllNginxes("nginx-storage")

// earlier: ${nginxConfigDir}/nginx.conf
tools.command("sudo nginx -c nginx.conf -p ${nginxConfigDir}")

// get my public port
def logBankPublicPort = tools.env['PUBLIC_STOMANPORT']
println "StorageManager LogBank: my external port is ${logBankPublicPort}"

// first, deregister this Storage from IS (because registering the same address causes error)
tools.deregisterServiceInIS('storage_managers', "${tools.thisHost}:${logBankPublicPort}")
tools.registerServiceInIS('storage_managers', "${tools.thisHost}:${logBankPublicPort}")

// TODO: use port from CAMEL model
def probe_delay_ms = 10000
try {
    while (true) {
        sleep(probe_delay_ms)
        tools.serviceStatusCommand("${tools.thisHostDocker}:20001")
        tools.mongoStatusCommand()
    }
} catch (Exception e) {
    def log_name = tools.config.railsEnv ? tools.config.railsEnv : 'development'
    tools.command("cat log/${log_name}.log", tools.serviceDir)
    throw e
}
