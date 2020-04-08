#!/bin/bash
jarFile=$(find ../target/ -name "*-spring-boot.jar")

# Copy Unit & JAR
scp -i ~/.ssh/id_rsa_student_mgmt_backend "sparky-spring-boot.service" elscha@147.172.178.30:~/.config/systemd/user
scp -i ~/.ssh/id_rsa_student_mgmt_backend "${jarFile}" elscha@147.172.178.30:~/Sparky/sparkyservice-spring-boot.jar

# Update service
ssh -i ~/.ssh/id_rsa_student_mgmt_backend elscha@147.172.178.30 'systemctl --user daemon-reload'
ssh -i ~/.ssh/id_rsa_student_mgmt_backend elscha@147.172.178.30 'systemctl --user restart sparky-spring-boot'
