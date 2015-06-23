def tools = new Tools(this.args)


tools.watchServiceStatus("localhost:3001", 1000)
