def tools = new Tools(this.args)
 
tools.optionalCommand('rake service:stop_single', tools.serviceDir, [
    'RAILS_ENV': 'production',
    'IS_URL': "${tools.getIsHost()}:${tools.isPort}",
    'IS_USER': tools.config.isUser,
    'IS_PASS': tools.config.isPass
])

// force killall mongod
tools.command("killall mongod || true")

// TODO: get mongo router public port
def mongodbPublicPort = tools.env['STOMANDBPORT_EXTERNAL_PORT']
println "StorageManager MongoDB: my external port is ${mongodbPublicPort}"
tools.deregisterServiceInIS("db_routers", "${tools.config.thisHost}:${mongodbPublicPort}")

// get my public port
def logBankPublicPort = tools.env['STOMANPORT_EXTERNAL_PORT']
println "StorageManager LogBank: my external port is ${logBankPublicPort}"

tools.deregisterServiceInIS('storage_managers', "${tools.thisHost}:${logBankPublicPort}")

tools.killAllNginxes("nginx-storage")
