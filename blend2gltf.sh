#!/bin/sh

for s in assets/*.blend; do
	blender "$s" --background --enable-autoexec --python assets/gltf_export.py -- ${PWD}/game/src/main/resources/
done

for s in assets/test/*.blend; do
	blender "$s" --background --enable-autoexec --python assets/gltf_export.py -- ${PWD}/engine/src/test/resources/
done
