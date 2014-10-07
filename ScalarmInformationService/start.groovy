def tools = new Tools(this.args)

tools.command('RAILS_ENV=production rake service:start', tools.serviceDir)
