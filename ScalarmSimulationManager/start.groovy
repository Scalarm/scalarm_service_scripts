def tools = new Tools(this.args)

tools.waitForExperimentManager()

def configStr = """\
{
	\"information_service_url\":\"${tools.isHost}:${tools.isPort}\",
	\"experiment_manager_user\":\"${tools.config.emUser}\",
	\"experiment_manager_pass\":\"${tools.config.emPassword}\",
	\"insecure_ssl\": true
}
"""

new File("${tools.installDir}/config.json").withWriter { out ->
    out.writeLine(configStr)
}

// execution moved to .sh file
tools.command("bash --login -c \"./scalarm_simulation_manager\"", tools.installDir)
// pushd $HOME/.cloudify/scalarm_simulation_manager
    // bash --login -c "./scalarm_simulation_manager"
// popd
