#!/bin/sh

/usr/bin/env JAVA_HOME=/usr/lib/jvm/java-1.8.0/ mvn clean compile assembly:single -P lwjgl-natives-linux-amd64,lwjgl-natives-linux-aarch64,lwjgl-natives-linux-arm32,lwjgl-natives-windows-amd64,lwjgl-natives-windows-x86,lwjgl-natives-macos-amd64,lwjgl-natives-macos-aarch64
