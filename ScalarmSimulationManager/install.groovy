def tools = new Tools(this.args)

tools.command("sudo apt-get -y install zip")

if (!tools.isRubyValid()) tools.installRvmRuby()

ant = new AntBuilder()

ant.copy(todir: tools.serviceDir) {
    fileset(dir: "scalarm_simulation_manager")
}
