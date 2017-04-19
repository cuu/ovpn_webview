#!/bin/bash

set -x 

adb uninstall net.openvpn.openvpn
adb install -r bin/MyName-debug.apk  
