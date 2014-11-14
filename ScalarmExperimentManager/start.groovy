def tools = new Tools(args)

tools.waitForInformationService()
tools.waitForStorageManager()

def nginxDir = "${tools.installDir}/nginx-experiment"

def ant = new AntBuilder()

tools.optionalCommandEnvsByConfig("rake service:stop")

// test for local development purposes - mongodb router could be already launched
if (!tools.isPortOccupied('localhost', 27017)) {

    ant.chmod(
        file: "${tools.serviceDir}/bin/mongos",
        perm: "a+x"
    )
    
    tools.commandEnvsByConfig("rake db_router:start")
}

// Start EM
// TODO: błąd, jeśli puma jest już uruchomiona, to rake service:start próbuje się uruchomić i pada z exitcode = 1
// proces rake zawisa (nie wiadomo dlaczego)
tools.commandEnvsByConfig("rake service:start")

// TODO create single user in Scalarm

// TODO ---- zmiany w samym Scalarmie
// - tryb cloudify - single user login (pomijanie login screen i logowanie od razu na użytkownika podanego w konfiguracji, ew. podanie hasła), use only anonymous
// single user mode -> całkowite pominięcie uwierzytelniania (login screen)  
// przenieść do zewnętrznego pliku konfiguracyjnego ustawianie, które infrastrutury mają być dostępne?
// TODO patch scalarm to support single-user installation


tools.killAllNginxes("nginx-experiment")
tools.command("sudo nginx -c nginx.conf -p ${nginxDir}")

// TODO: fail if not {"status":"ok"...}
// assert ant.project.properties.cmdOut1 ==~ /.*"status":"ok".*/

// Deregister this EM from IS (because registering the same address causes error)
tools.deregisterExperimentManager()
tools.registerExperimentManager()

// register example scenario
tools.command("curl https://${tools.thisHost}/simulations -k -u anonymous:pass123 -F simulation_name=\"Molecular Dynamics\" -F simulation_description=\"Molecular Dynamics simulation\" -F simulation_binaries=@\"simulation/bin.zip\" -F simulation_input=@\"simulation/input.json\" -F executor=@\"simulation/executor\" -F input_writer=@\"simulation/input_writer\" -F output_reader=@\"simulation/output_reader\" -F progress_monitor=@\"simulation/progress_monitor\"", tools.installDir)

println "[OK] Finished start script"
