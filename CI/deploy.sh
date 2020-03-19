#!/bin/bash
jarName=sparkyservice-api-*-spring-boot.jar
scp -i ~/.ssh/id_rsa_student_mgmt_backend ../sparkyservice-api/target/"${jarName}" elscha@147.172.178.30:~/Sparky
ssh -i ~/.ssh/id_rsa_student_mgmt_backend elscha@147.172.178.30 'systemctl --user restart sparky-spring-boot'
