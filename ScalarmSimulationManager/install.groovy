def tools = new Tools(this.args)

tools.command("sudo apt-get -y install zip")

tools.download(tools.config.simDownloadURL, "${tools.installDir}/scalarm_simulation_manager.xz")
tools.command("unxz scalarm_simulation_manager.xz && chmod +x scalarm_simulation_manager", tools.installDir)

ant = new AntBuilder()

