def tools = new Tools(this.args)

tools.command('RAILS_ENV=production rake service:stop', tools.serviceDir)
