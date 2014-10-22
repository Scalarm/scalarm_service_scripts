def tools = new Tools(args)

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

ant.copy(file:"scalarm.yml", todir: tools.serviceConfigDir)
ant.copy(file:"secrets.yml", todir: tools.serviceConfigDir)
ant.copy(file:"puma.rb", todir: tools.serviceConfigDir)

tools.command("bundle install", tools.serviceDir)

ant.mkdir(dir: "${tools.serviceDir}/log")

//TODO? r-cran-class r-cran-mass r-cran-nnet r-cran-spatial
tools.command("sudo apt-get -y install r-base-core sysstat")

tools.commandProduction("RAILS_ENV=production rake service:non_digested")

