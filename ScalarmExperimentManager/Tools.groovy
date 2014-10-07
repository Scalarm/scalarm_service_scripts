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
            println "Usage: groovy script <this_host_local_network_address> <information_service_address>"
            throw new RuntimeException("invalid script arguments")
        }
    
        instanceId = 1 // TODO
        config = new ConfigSlurper().parse(new File("ScalarmExperimentManager-service.properties").toURL())
        installDir = System.properties["user.home"]+ "/.cloudify/${config.serviceName}"
        serviceDir = "${installDir}/${config.serviceName}"
        serviceConfigDir = "${serviceDir}/config"
        
        println config
        
        new AntBuilder().mkdir(dir: installDir) // works like mkdir -p

        thisHost = args[0]
        isHost = args[1]
        
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
    
    def killAllNginxes() {
        ServiceUtils.ProcessUtils.getPidsWithQuery("Args.0.re=nginx.*master process nginx.*nginx-experiment.*").each { pid ->
            optionalCommand("sudo kill ${pid}")
        }
    }
    
    String getIsHost() {
        "localhost"
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