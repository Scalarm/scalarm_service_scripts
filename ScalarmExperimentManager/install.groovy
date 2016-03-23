def tools = new Tools(args)

tools.installCurl()

def nginxDir = "${tools.installDir}/nginx-experiment"

if (!tools.isRubyValid()) tools.installRvmRuby()
if (!tools.isNginxPresent()) tools.installNginx()
tools.installGit()

def ant = new AntBuilder()

ant.mkdir(dir: nginxDir)
ant.mkdir(dir: "${nginxDir}/logs")
ant.copy(todir: nginxDir) {
    fileset(dir: "nginx-experiment")
}

// download Experiment Manager's code
ant.sequential {
    mkdir(dir: tools.installDir)
    tools.download(tools.config.downloadPath, "${tools.installDir}/em.zip")
}

// TODO: scalarm_experiment_manager-master is a directory in from ZIP
// change if GIT branch changes (e.g. to master)
ant.unzip(src:"${tools.installDir}/em.zip", dest: tools.installDir, overwrite:true)
ant.move(file:"${tools.installDir}/${tools.config.serviceName}-${tools.config.scalarmTag}", tofile: tools.serviceDir)

// scalarm.yml is not used from 15.06
//ant.copy(file:"scalarm.yml", todir: tools.serviceConfigDir)

// TODO: mongodb credentials in secrets.database are hardcoded
ant.copy(file:"secrets.yml", todir: tools.serviceConfigDir)
ant.copy(file:"puma.rb", todir: tools.serviceConfigDir)

// patches disabled, because changes are on paasage branch
// Copy and apply PaaSage-specific patches
// tools.copyAndApplyPatch("disable_workers_packages.patch")

tools.command("bundle install", tools.serviceDir)

ant.mkdir(dir: "${tools.serviceDir}/log")

//TODO? r-cran-class r-cran-mass r-cran-nnet r-cran-spatial
tools.command("sudo apt-get -y install r-base-core sysstat")

if (tools.config.railsEnv == 'production') {
    tools.commandEnvsByConfig("rake service:non_digested")
}

ant.mkdir(dir: "${tools.installDir}/simulation")

tools.download(tools.config.simulationBinariesDownloadPath, "${tools.installDir}/simulation/bin.zip")

ant.copy(todir: "${tools.installDir}/simulation") {
    fileset(dir: "simulation")
}
