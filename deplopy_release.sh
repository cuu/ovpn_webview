#!/bin/bash

ant release

if [ $? -eq 0 ]; then
  ./sign.sh 
	adb uninstall net.openvpn.openvpn
	adb install -r bin/MyName-release-guusigned.apk
fi


   




