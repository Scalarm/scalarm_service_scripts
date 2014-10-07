def tools = new Tools(args)

def nginxDir = "${tools.installDir}/nginx-experiment"

def ant = new AntBuilder()

tools.optionalCommandProduction("rake service:stop")

// test for local development purposes - mongodb router could be already launched
if (!tools.isPortOccupied('localhost', 27017)) {

    ant.chmod(
        file: "${tools.serviceDir}/bin/mongos",
        perm: "a+x"
    )
    
    tools.commandProduction("rake db_router:start")
}

// Start EM
// TODO: błąd, jeśli puma jest już uruchomiona, to rake service:start próbuje się uruchomić i pada z exitcode = 1
// proces rake zawisa (nie wiadomo dlaczego)
tools.commandProduction("rake service:start")

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

println "[OK] Finished start script"
