import bpy
import os
import sys
import os.path as path

try:
    argv = sys.argv
    if "--" in argv:
        argv = argv[argv.index("--") + 1:]  # get all args after "--"
    else:
        argv = []

    respath = argv[0]

    scene_name = path.splitext(path.basename(bpy.data.filepath))[0]

    tex_dir = path.join(respath, "textures", "gltf", scene_name);
    gltf_dir = path.join(respath, "gltf")
    out_path = path.join(gltf_dir, scene_name + ".gltf");

    if not path.exists(respath):
        os.makedirs(respath)
    if not path.exists(tex_dir):
        os.makedirs(tex_dir)
    if not path.exists(gltf_dir):
        os.makedirs(gltf_dir)

    bpy.ops.export_scene.gltf(export_format='GLTF_SEPARATE', filepath=out_path, export_texture_dir=tex_dir, export_cameras=True, export_yup=False, export_lights=True)

except Exception as err:
    print(err, file=sys.stderr)
    sys.exit(1)

