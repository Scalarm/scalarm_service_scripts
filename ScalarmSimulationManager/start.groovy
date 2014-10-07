def tools = new Tools(this.args)

def configStr = "{\"information_service_url\":\"${tools.getIsHost()}:${tools.config.isPort}\",\"experiment_manager_user\":\"${tools.config.emUser}\",\"experiment_manager_pass\":\"${tools.config.emPassword}\"}"

new File("${tools.serviceDir}/config.json").withWriter { out ->
    out.writeLine(configStr)
}

tools.command("nohup ruby simulation_manager.rb >sim.log 2>&1 </dev/null & echo \$!", tools.serviceDir)
