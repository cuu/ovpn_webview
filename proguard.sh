#!/bin/bash


echo "-useuniqueclassmembernames"
echo "-keepattributes SourceFile,LineNumberTable"
echo "-allowaccessmodification"

echo "-libraryjars ./libs/thindownloadmanager-1.0.3.jar"

cat <<EOF
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclasseswithmembernames class * { 
    native <methods>;
}
EOF

for filename in src/net/openvpn/openvpn/*.java; do
	FF=$(basename "$filename")
	CLASS=`echo $FF| cut -d "." -f1`
	echo "-keep class net.openvpn.openvpn.$CLASS {*;}"

done


