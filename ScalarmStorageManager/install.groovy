def tools = new Tools(this.args)

tools.installCurl()

def nginxDir = "${tools.installDir}/nginx-storage"

if (!tools.isRubyValid()) tools.installRvmRuby()
if (!tools.isNginxPresent()) tools.installNginx()

def ant = new AntBuilder()

// copy nginx configuration
ant.sequential() {
    mkdir(dir: nginxDir)
    mkdir(dir: "${nginxDir}/logs")
    copy(todir: nginxDir) {
        fileset(dir: "nginx-storage")
    }
}

// download Storage Manager's code
ant.sequential {
    tools.download("${tools.config.downloadPath}", "${tools.installDir}/archive.zip")
    unzip(src:"${tools.installDir}/archive.zip", dest: tools.installDir, overwrite: true)
    def dirInPackage = "${tools.installDir}/${tools.config.serviceName}-${tools.config.scalarmTag}"
    move(file: dirInPackage, tofile: tools.serviceDir)
}

// copy config files
def configFiles = ['secrets.yml', 'thin.yml']
configFiles.each { ant.copy(file: it, todir: tools.serviceConfigDir) }

def scalarmYML = """\
mongo_host: ${tools.thisHost}
mongo_port: 27017
db_name: 'scalarm_db'
binaries_collection_name: 'simulation_files'
host: ${tools.thisHost}
db_instance_port: 30000
db_instance_dbpath: ./../../scalarm_db_data
db_instance_logpath: ./../../log/scalarm_db.log
db_config_port: 28000
db_config_dbpath: ./../../scalarm_db_config_data
db_config_logpath: ./../../log/scalarm_db_config.log
db_router_host: localhost
db_router_port: 27017
db_router_logpath: ./../../log/scalarm_db_router.log
monitoring:
  db_name: 'scalarm_monitoring'
  metrics: ''
  interval: 60
"""

new File("${tools.serviceConfigDir}/scalarm.yml").withWriter { out ->
    out.writeLine(scalarmYML)
}


// download MongoDB's binaries
tools.download("${tools.config.mongodbDownloadUrl}", "${tools.installDir}/mongodb.tgz")
tools.command('tar zxvf mongodb.tgz', tools.installDir)
ant.move(file:"${tools.installDir}/mongodb-${tools.config.osName}-x86_64-${tools.config.mongodbVersion}", tofile: "${tools.serviceDir}/mongodb")

tools.command("bundle install", tools.serviceDir)
