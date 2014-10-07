import java.util.concurrent.TimeUnit

public class Tools
{
    def instanceId
    def config
    def installDir
    def serviceDir
    def serviceConfigDir
    def thisHost
    def isHost
    
    def Tools(args) {
        if (args.size() < 2) {
            println "Usage: groovy script <config_file> <this_host_local_network_address> <information_service_address>"
            throw new RuntimeException("invalid script arguments")
        }
    
        instanceId = 1 // TODO
        config = new ConfigSlurper().parse(new File(args[0]).toURL())
        installDir = System.properties["user.home"]+ "/.cloudify/${config.serviceName}"
        serviceDir = "${installDir}/${config.serviceName}"
        serviceConfigDir = "${serviceDir}/config"
        
        println config
        
        new AntBuilder().mkdir(dir: installDir) // works like mkdir -p

        thisHost = args[1]
        isHost = args[2]
        
        println "this: ${thisHost}; isHost: ${isHost}"
    }
    
    def installCurl() {
        command("sudo apt-get -y install curl")
    }
    
    void installGit() {
        command("sudo apt-get -y install git")
    }
    
    def deregisterExperimentManager() {
        execute('curl', installDir, false, [
            '--user', 'scalarm:scalarm',
            '-k', '-X', 'POST', "https://${getIsHost()}:${config.isPort}/experiments/deregister",
            '--data', "address=${thisHost}:443"
        ])
    }
    
    def registerExperimentManager() {
        execute('curl', installDir, true, [
            '--user', 'scalarm:scalarm',
            '-k', '-X', 'POST', "https://${getIsHost()}:${config.isPort}/experiments/register",
            '--data', "address=${thisHost}:443"
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
        def installCmd = "\\curl -sSL https://get.rvm.io | bash -s stable --ruby=2.1"
        command(installCmd)['out']
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

    def deregisterStorageManager() {
        execute('curl', installDir, false, [
            '--user', 'scalarm:scalarm',
            '-k', '-X', 'POST', "https://${getIsHost()}:${config.isPort}/storage/deregister",
            '--data', "address=${thisHost}:${config.logBankPort}"
        ])
    }
    
    def registerStorageManager() {
        execute('curl', installDir, true, [
            '--user', 'scalarm:scalarm',
            '-k', '-X', 'POST', "https://${getIsHost()}:${config.isPort}/storage/register",
            '--data', "address=${thisHost}:${config.logBankPort}"
        ])
    }
    
    void commandProduction(cmd) {
        command(cmd, serviceDir, envsProduction())
    }
    
    void optionalCommandProduction(cmd) {
        optionalCommand(cmd, serviceDir, [
            'RAILS_ENV': 'production',
            'IS_URL': "${getIsHost()}:${config.isPort}",
            'IS_USER': 'scalarm',
            'IS_PASS': 'scalarm'
        ])
    }
    
    def envsProduction() {
        [
            'RAILS_ENV': 'production',
            'IS_URL': "${getIsHost()}:${config.isPort}",
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