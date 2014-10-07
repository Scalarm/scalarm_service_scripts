def tools = new Tools(this.args)

// ---

tools.installCurl()

if (!tools.isRubyValid()) tools.installRvmRuby()

def configFiles = ['secrets.yml', 'scalarm-cert.pem', 'scalarm-cert-key.pem', 'thin.yml']

tools.command("wget ${tools.config.downloadPath} -O ${tools.installDir}/archive.zip")

new AntBuilder().sequential {
    unzip(src: "${tools.installDir}/archive.zip", dest: tools.installDir, overwrite: true)
    move(file: "${tools.installDir}/${tools.config.serviceName}-${tools.config.scalarmTag}", tofile: tools.serviceDir)  
    
    configFiles.each() { copy(file: it, todir: tools.serviceConfigDir) }
}

tools.command('bundle install', tools.serviceDir)
tools.command('rake db:migrate', tools.serviceDir, ['RAILS_ENV': 'production'])
tools.command('rake db:migrate', tools.serviceDir)

