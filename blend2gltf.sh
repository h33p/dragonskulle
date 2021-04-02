#!/bin/sh

rm game/src/main/resources/textures/gltf/*
rm game/src/main/resources/gltf/*

for s in assets/*.blend; do
	blender "$s" --background --enable-autoexec --python assets/gltf_export.py -- ${PWD}/game/src/main/resources/
done

rm engine/src/test/resources/textures/gltf/*
rm engine/src/test/resources/gltf/*

for s in assets/test/*.blend; do
	blender "$s" --background --enable-autoexec --python assets/gltf_export.py -- ${PWD}/engine/src/test/resources/
done
