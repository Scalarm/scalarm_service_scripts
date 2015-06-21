def tools = new Tools(this.args)

tools.waitForInformationService()

def nginxConfigDir = "${tools.installDir}/nginx-storage"

// generate config file (we are sure that we know thisHost)
// mongo_host - assume that mongo is running within this instance and listens on thisHost private port
// 
def scalarmYML = """\
# log_bank
mongo_host: ${tools.thisHost}
mongo_port: 27017
db_name: 'scalarm_db'
binaries_collection_name: 'simulation_files'
# mongodb
host: ${tools.thisHost}
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
    'IS_URL': "${tools.getIsHost()}:${tools.config.isPort}",
    'IS_USER': tools.config.isUser,
    'IS_PASS': tools.config.isPass
])

// start single mongo instance (mongod) and log_bank
// will listen on db_router_port and 
tools.command('rake service:start_single', tools.serviceDir, [
    'RAILS_ENV': 'production',
    'IS_URL': "${tools.getIsHost()}:${tools.config.isPort}",
    'IS_USER': tools.config.isUser,
    'IS_PASS': tools.config.isPass
])

// Kill found nginx-storage processes
tools.killAllNginxes("nginx-storage")

// earlier: ${nginxConfigDir}/nginx.conf
tools.command("sudo nginx -c nginx.conf -p ${nginxConfigDir}")

// first, deregister this Storage from IS (because registering the same address causes error)
tools.deregisterStorageManager()
tools.registerStorageManager()
