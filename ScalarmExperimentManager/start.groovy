def tools = new Tools(args)

tools.waitForInformationService()
tools.waitForStorageManager()

def nginxDir = "${tools.installDir}/nginx-experiment"

def ant = new AntBuilder()

tools.optionalCommandEnvsByConfig("rake service:stop")

// db_router disabled - using mongodb residing on other host
// test for local development purposes - mongodb router could be already launched
//if (!tools.isPortOccupied('localhost', 27017)) {
//
//    ant.chmod(
//        file: "${tools.serviceDir}/bin/mongos",
//        perm: "a+x"
//    )
//    
//    tools.commandEnvsByConfig("rake db_router:start")
//}

// Start EM
// TODO: błąd, jeśli puma jest już uruchomiona, to rake service:start próbuje się uruchomić i pada z exitcode = 1
// proces rake zawisa (nie wiadomo dlaczego)
tools.commandEnvsByConfig("rake service:start")

// TODO: przenieść do zewnętrznego pliku konfiguracyjnego ustawianie, które infrastrutury mają być dostępne?

tools.killAllNginxes("nginx-experiment")
tools.command("sudo nginx -c nginx.conf -p ${nginxDir}")

// TODO: fail if not {"status":"ok"...}
// assert ant.project.properties.cmdOut1 ==~ /.*"status":"ok".*/

// Deregister this EM from IS (because registering the same address causes error)

// get my public port
def emPublicPort = tools.env['PUBLIC_EXPMANPORT']
println "ExperimentManager: my external port is ${emPublicPort}"

tools.deregisterServiceInIS('experiment_managers', "${tools.thisHost}:${emPublicPort}")
tools.registerServiceInIS('experiment_managers', "${tools.thisHost}:${emPublicPort}")

println "Waiting 5 seconds for ExperimentManager to settle down..."
sleep(5000)

// register example scenario
// TODO: EM credentials in config?
tools.command("curl https://${tools.thisHost}/simulations -k -u anonymous:pass123 -F simulation_name=\"Molecular Dynamics\" -F simulation_description=\"Molecular Dynamics simulation\" -F simulation_binaries=@\"simulation/bin.zip\" -F simulation_input=@\"simulation/input.json\" -F executor=@\"simulation/executor\" -F input_writer=@\"simulation/input_writer\" -F output_reader=@\"simulation/output_reader\" -F progress_monitor=@\"simulation/progress_monitor\"", tools.installDir)

println "[OK] Finished start script, now will check status periodically"

// TODO: use port from CAMEL model
tools.watchServiceStatus("${tools.thisHostDocker}:443")
