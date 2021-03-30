@ECHO OFF
setlocal enabledelayedexpansion
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

