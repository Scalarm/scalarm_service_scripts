def tools = new Tools(this.args)

tools.command("sudo apt-get -y install ruby zip bc mencoder wget libboost-dev libboost-thread-dev libtiff5-dev libopenexr-dev curl wget")

tools.command("wget http://ftp.sa.xemacs.org/pub/ubuntu-releases/ubuntu/pool/universe/p/povray/povray_3.7.0.0-4~ubuntu14.04.1_amd64.deb -O /tmp/povray37.deb")
tools.command("wget http://ftp.sa.xemacs.org/pub/ubuntu-releases/ubuntu/pool/universe/p/povray/povray-includes_3.7.0.0-4~ubuntu14.04.1_all.deb  -O /tmp/povray37_include.deb")

tools.command("dpkg -i /tmp/povray37.deb")
tools.command("dpkg -i /tmp/povray37_include.deb")

// to execute sample simulation, which has executor in ruby
if (!tools.isRubyValid()) tools.installRvmRuby()

tools.download(tools.config.simDownloadURL, "${tools.installDir}/scalarm_simulation_manager.xz")
tools.command("unxz scalarm_simulation_manager.xz && chmod +x scalarm_simulation_manager", tools.installDir)

ant = new AntBuilder()
