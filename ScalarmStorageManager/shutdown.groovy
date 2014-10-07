def tools = new Tools(this.args)

new AntBuilder().delete(dir: tools.installDir)
