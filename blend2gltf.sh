#!/bin/sh

for s in assets/*.blend; do
	blender "$s" --background --python assets/gltf_export.py -- ${PWD}/game/src/main/resources/
done
