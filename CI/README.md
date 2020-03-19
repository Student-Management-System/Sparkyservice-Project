This directory contains script and files for our continuous integration setup with jenkins.

- `sparky-spring-boot.service`: Systemd user unit for starting an test instance of `sparkyservice-api` project
- `deploy.sh`: Jenkins uses this script to deploy a runnable jar to a remote server where a webserver is started

# Tomcat Preparations
The integration tests of `sparkyservice-api` needs docker and permissions on it.

**Ubuntu 18.04**:

    apt install docker.io
    gpasswd -a jenkins docker
    
# Live instance
After each successful build, we provide a runnable jar with an embedded tomcat server of `sparkyservice-api` and deploy it for live testing. 

On the machine where the instance should be running:

- Java must be installed
- We use a systemd (user) unit to start the server. Move `sparky-spring-boot.service` to `~/.config/systemd/user/` and reload with `systemctl --user daemon-reload`, start and enabled afterwards with `systemctl --user enable sparky-spring-boot`
    - on ubuntu this should work out of the box
    - otherwise see an instruction below
- Configure project in jenkins to run `deploy.sh` as post build shell script
- The jenkins user must have access to the remote maschine (maybe you want to an ssh key)
- Create `application-release.properties` on the maschine where the live instance is running. The file must be in the same directory as the jar. 

## Configurations


### Enables a login shell for users.
Maybe not necessary on Ubuntu machines. Only if the user can't run `systemd --user` with an error message `[...] can't connect to DBUS`

    sudo loginctl enable-linger moin
    echo 'export XDG_RUNTIME_DIR="/run/user/$UID"' >> ~/.bashrc
    echo 'export DBUS_SESSION_BUS_ADDRESS="unix:path=${XDG_RUNTIME_DIR}/bus"' >> ~/.bashrc
    
    
### Remote Setup

You need port 8080 free and open.

    adduser elscha
    mkdir /home/elscha/.ssh
    echo "public ssh key" >> /home/elscha/.ssh/authorized_keys
    # java invallation...
    su -l elscha
    mkdir ~/Sparky # the target deploy directory from jenkins

    
### Unit installation

    mkdir -p ~/.config/systemd/user
    cp /path/to/git/CI/sparky-spring-boot.service ~/.config/systemd/user/
    systemctl --user daemon-reload
    systemctl --user enable sparky-spring-boot
    systemctl --user start sparky-spring-boot

    
