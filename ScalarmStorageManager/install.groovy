def tools = new Tools(this.args)

tools.installCurl()

def nginxDir = "${tools.installDir}/nginx-storage"

if (!tools.isRubyValid()) tools.installRvmRuby()
if (!tools.isNginxPresent()) tools.installNginx()
tools.installGit()

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



// download MongoDB's binaries
tools.download("${tools.config.mongodbDownloadUrl}", "${tools.installDir}/mongodb.tgz")
tools.command('tar zxvf mongodb.tgz', tools.installDir)
ant.move(file:"${tools.installDir}/mongodb-${tools.config.osName}-x86_64-${tools.config.mongodbVersion}", tofile: "${tools.serviceDir}/mongodb")

tools.command("bundle install", tools.serviceDir)
