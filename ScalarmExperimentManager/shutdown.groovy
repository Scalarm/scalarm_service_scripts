def tools = new Tools(args)

def ant = new AntBuilder()

ant.delete(dir: tools.installDir)
