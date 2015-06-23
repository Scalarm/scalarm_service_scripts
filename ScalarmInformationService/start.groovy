def tools = new Tools(this.args)

tools.command('RAILS_ENV=production rake service:start', tools.serviceDir)

// TODO: use port from CAMEL model
tools.watchServiceStatus("${tools.thisHostDocker}:11300")
