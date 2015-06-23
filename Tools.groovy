import java.util.concurrent.TimeUnit

public class Tools
{
    def instanceId
    def config
    def installDir
    def serviceDir
    def serviceConfigDir
    def thisHost
    def thisHostDocker
    def isHost
    def isPort
    def storageHost
    def storagePort
    def emHost
    def emPort
    def env
    
    def Tools(args) {
        if (args.size() < 1) {
            println "Usage: groovy script <config_file>"
        }
    
        env = System.getenv()
    
        //if (args.size() < 2) {
        //    println "Usage: groovy script <config_file> <this_host_local_network_address> <information_service_address/service_adresses>"
        //    throw new RuntimeException("invalid script arguments")
        //}
    
        instanceId = 1 // TODO
        config = new ConfigSlurper().parse(new File(args[0]).toURL())
        installDir = System.properties["user.home"]+ "/.cloudify/${config.serviceName}"
        serviceDir = "${installDir}/${config.serviceName}"
        serviceConfigDir = "${serviceDir}/config"

        println "Config:"
        println config
        
        new AntBuilder().mkdir(dir: installDir) // works like mkdir -p

        // deprecated - old version
        // thisHost = args[1]
        
        // new version of CAMEL
        thisHost = env['CONTAINER_HOST_IP']
        thisHostDocker = env['LOCAL_IP']
        
        // TODO: get my port from eg. env[EXPMANPORT_EXTERNAL_PORT]

        if (env.containsKey("INFSERPORTREQ")) {
            def isAddress = env["INFSERPORTREQ"].split(',')[0]
            isHost = isAddress.split(':')[0]
            isPort = isAddress.split(':')[1]
        }
        
        if (env.containsKey('STOMANPORTREQ')) {
            def stAddress = env["STOMANPORTREQ"].split(',')[0]
            storageHost = stAddress.split(':')[0]
            storagePort = stAddress.split(':')[1]
        }
        
        if (env.containsKey('EXPMANPORTREQ')) {
            def emAddress = env["EXPMANPORTREQ"].split(',')[0]
            emHost = emAddress.split(':')[0]
            emPort = emAddress.split(':')[1]
        }
        
        println "serviceDir: ${serviceDir}"
        println "this: ${thisHost}; thisDocker: ${thisHostDocker}"
        println "isHost: ${isHost}, isPort: ${isPort}"
        println "storageHost: ${storageHost}, storagePort: ${storagePort}"
        println "emHost: ${emHost}, emPort: ${emPort}"

    }
    
    // TODO: ports set constant
    
    def waitForService(address, port, name) {
        while (!isPortOccupied(address, port)) {
            println "Waiting for ${name}..."
            sleep(5000)
        }
    }
    
    def waitForInformationService() {
        waitForService(isHost, isPort, "Information Service")
    }
    
    def waitForStorageManager() {
        waitForService(storageHost, storagePort, "Storage Manager")
    }
    
    def waitForExperimentManager() {
        waitForService(emHost, emPort, "Experiment Manager")
    }
    
    // Address without https://, eg.: localhost:3001
    def watchServiceStatus(address, probe_delay_ms=10000) {
        try {
            while (true) {
                sleep(probe_delay_ms)
                serviceStatusCommand(address)
            }
        } catch (Exception e) {
            def log_name = config.railsEnv ? config.railsEnv : 'development'
            command("cat log/${log_name}.log", serviceDir)
            throw e
        }
    }

    def serviceStatusCommand(address) {
        tools.command("bash check_service.sh ${address}", '.')
    }

    def mongoStatusCommand() {
        tools.command("bash check_mongod.sh")
    }
    
    def installCurl() {
        command("sudo apt-get -y install curl")
    }
    
    void installGit() {
        command("sudo apt-get -y install git")
    }
    
    def registerServiceInIS(name_plural, address) {
        execute('curl', installDir, false, [
            '--user', 'scalarm:scalarm', // TODO
            '-k', '-X', 'POST', "https://${isHost}:${isPort}/${name_plural}",
            '--data', "address=${address}"
        ])
    }
    
    def deregisterServiceInIS(name_plural, address) {
        execute('curl', installDir, false, [
            '--user', 'scalarm:scalarm', // TODO
            '-k', '-X', 'DELETE', "https://${isHost}:${isPort}/${name_plural}",
            '--data', "address=${address}"
        ])
    }
            
    def getIsHost() {
        isHost
    }
    
    boolean isRubyValid() {
        def p = optionalCommand('ruby -v')
        
        p['exit'] == 0 && p['out'] =~ /ruby 2\.1.*/
    }

    def installRvmRuby() {
        println 'installing RVM...'
        optionalCommand('gpg --keyserver hkp://keys.gnupg.net --recv-keys D39DC0E3')
        def installCmd = "\\curl -sSL https://get.rvm.io | bash -s stable --ruby=2.1"
        command(installCmd)['out']
    }

    def copyAndApplyPatch(patch_file_name) {
        def ant = new AntBuilder()
        ant.copy(file: patch_file_name, todir: serviceDir)
        command("patch -p1 < ${patch_file_name}", serviceDir)
    }

    def command(command, dir=installDir, envs=[], failonerror=true) {
        execute('bash', dir, failonerror, ['--login', '-c', command], envs)
    }

    def optionalCommand(cmd, dir=installDir, envs=[]) {
        command(cmd, dir, envs, false)
    }

    boolean isNginxPresent() {
        def p = optionalCommand('nginx -v')
        p['exit'] == 0
    }

    void installNginx() {
        def cmd = [
            "sudo apt-get -y install python-software-properties software-properties-common",
            "sudo add-apt-repository -y ppa:nginx/stable",
            "sudo apt-get update",
            "sudo apt-get install -y nginx"
        ].join(" && ")
        
        command(cmd)
    }
    
    def killAllNginxes(name) {
        getPids("nginx.*master process nginx.*${name}.*").each { pid -> optionalCommand("sudo kill ${pid}") }
    }
    
    def killAllSimulationManagers() {
        getSimulationManagerPids().collect { pid ->
            optionalCommand("sudo kill ${pid}")
        }
    }

    def getSimulationManagerPids() {
        getPids("ruby.*simulation_manager.rb")
    }
    
    def commandProduction(cmd) {
        commandEnvs(cmd, 'production')
    }

    def commandEnvsByConfig(cmd) {
        commandEnvs(cmd, config.railsEnv)
    }
    
    def commandEnvs(cmd, railsEnv) {
        command(cmd, serviceDir, envsFor(railsEnv))
    }

    def optionalCommandProduction(cmd) {
        optionalCommand(cmd, serviceDir, envsProduction())
    }

    def optionalCommandEnvsByConfig(cmd) {
        optionalCommand(cmd, serviceDir, envsFor(config.railsEnv))
    }
    
    def envsProduction() {
        envsFor('production')
    }

    def envsFor(railsEnv) {
        [
            'RAILS_ENV': railsEnv,
            'IS_URL': "${isHost}:${isPort}",
            'IS_USER': 'scalarm',
            'IS_PASS': 'scalarm'
        ]
    }
    
    def re_proc = /([0-9]+) .*/
        
    def getPids(query) {
        def ps = optionalCommand("ps -eo pid,args | tail -n +2 | grep \"[${query[0]}]${query[1..-1]}\"")
        def lines = ps['out'].split('\n')
        if (lines.size() == 1 && lines[0] == "") {
            []
        } else {
            lines.collect() { (it =~ re_proc)[0][1].toInteger() }
        }
    }
    
    def isPortOccupied(host, port) {
        def ps = optionalCommand("nc ${host} ${port} < /dev/null")
        ps["exit"] == 0
    }
 
    def download(address, outputFile) {
        command("wget \"${address}\" -O \"${outputFile}\"")
    }
 
    def execute(executable, dir, failonerror, args=[], envs=[]) {
        def cmd = "${executable} ${args.join(' ')}"
        println "executing: '${cmd}' in ${dir}"
        
        def ant = new AntBuilder()
        try {
            ant.exec(
                executable: executable,
                dir: dir,
                failonerror: failonerror,
                
                outputproperty: 'out',
                errorproperty: 'err',
                resultproperty: 'result'
            ) {
                args.each() { arg(value: it) }
                envs.each() { k, v -> env(key: k, value: v) }
            }
        } finally {
            println "finished: '${cmd}' with exit code ${ant.project.properties.result}"
            println "envs: ${envs}"
            println "- stdout: ${ant.project.properties.out}"
            println "- stderr: ${ant.project.properties.err}"
        }
        return [
            'out': ant.project.properties.out,
            'err': ant.project.properties.err, 
            'exit': Integer.parseInt(ant.project.properties.result)
        ]
    }
}
