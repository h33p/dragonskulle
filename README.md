
# Hex Wars

## University of Birmingham Computer Science team project.

This is our second year team project - a Vulkan and Java powered online 3D strategy game.

This game runs on Linux and Windows, where Vulkan is available.

macOS support is untested, as at the time we did not have hardware access. However, it should in theory run thanks to `MoltenVK`.

## Running

To compile and run the project, maven is required. Executing this command will run the game:

```
mvn clean compile exec:java
```

To build a portable JAR, execute the build script:

```
./build_all_jar.sh
```

The runnable JAR will be generated in `game/target/` subdirectory.

## Documentation

Generate Javadocs using this command:

```
mvn clean site
```

This will also generate additional maven site, which is not relevant for the purpose, but here are the important file paths:

`engine/target/site/apidocs/index.html` - for engine documentation.

`game/target/site/apidocs/index.html` - for game documentation.

Additional documentation is available in the [docs](/docs) subdirectory, containing more detailed guides for a few of our systems and workflow.

## Other important maven goals

* `spotless:check` - checks whether the code is formatted properly.
* `spotless:apply` - formats the code.
* `checkstyle:check` - additional documentation checks.

## Updating assets

If Blender are updated, it is necessary to regenerate the glTF assets. On Unix systems, run this script:

```
./blend2gltf.sh
```

On Windows, run the `wblend2fgltf.bat` script.

## Environment variables

Additional environment variables can be set before running the game to set low level renderer settings.

`TARGET_GPU=<substring>` will only pick a GPU that has the substring as part of its name. Examples:

`TARGET_GPU=RADV` will use AMD GPU on Linux.

`TARGET_GPU=llvm` will use the lavapipe software renderer.

`TARGET_GPU=Intel` will use the Intel GPU on Linux.

`TARGET_GPU=NVIDIA` will use the Nvidia GPU.

Setting `DEBUG_RENDERER=true` will enable Vulkan validation layers (Vulkan SDK required). `LOAD_RENDERDOC=true` will attempt to load renderdoc library.

