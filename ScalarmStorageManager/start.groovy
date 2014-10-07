def tools = new Tools(this.args)

def nginxConfigDir = "${tools.installDir}/nginx-storage"

// in case if one of service parts is still running after some failure
tools.optionalCommand('rake service:stop', tools.serviceDir, [
    'RAILS_ENV': 'production',
    'IS_URL': "${tools.getIsHost()}:${tools.config.isPort}",
    'IS_USER': tools.config.isUser,
    'IS_PASS': tools.config.isPass
])

tools.command('rake service:start', tools.serviceDir, [
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
