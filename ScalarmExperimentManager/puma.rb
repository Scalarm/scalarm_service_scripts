environment 'production'
daemonize

bind 'unix:///tmp/scalarm_experiment_manager.sock'
# bind 'tcp://0.0.0.0:3005'

stdout_redirect 'log/puma.log', 'log/puma.log.err', true
pidfile 'puma.pid'

threads 1,16
workers 4
#preload_app!
