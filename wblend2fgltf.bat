@ECHO OFF
setlocal enabledelayedexpansion
echo Cleaning gltf
del /q %CD%\\game\\src\\main\\resources\\textures\\gltf\\*
del /q %CD%\\game\\src\\main\\resources\\gltf\\*
del /q %CD%\\engine\\src\\test\\resources\\textures\\gltf\\*
del /q %CD%\\engine\\src\\test\\resources\\gltf\\*

for %%f in (assets\*.blend) do (
          set /p val=<%%f
          echo Exporting %%f to %CD%/game/src/main/resources/%%f
          blender %%f --background --enable-autoexec --python assets/gltf_export.py -- %CD%/game/src/main/resources/
          echo --------------
)
for %%f in (assets\test\*.blend) do (
          set /p val=<%%f
          echo Exporting %%f to %CD%/engine/src/test/resources/%%f
          blender %%f --background --enable-autoexec --python assets/gltf_export.py -- %CD%/engine/src/test/resources/
          echo --------------
)

