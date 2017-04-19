#!/bin/bash


FILE="MyName-release-unsigned.apk"
OUT="MyName-release-guusigned.apk"

signapk.sh -o bin/$OUT bin/$FILE

