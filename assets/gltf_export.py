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

    if len(argv) > 0:
        respath = argv[0]
    else:
        cwd = os.getcwd()
        os.chdir(bpy.path.abspath("//"))
        try:
            git_root = subprocess.check_output(['git', 'rev-parse', '--show-toplevel']).decode(encoding='UTF-8').strip()
        except:
            git_root = os.path.join(bpy.path.abspath("//"), "../")
        os.chdir(cwd)
        respath = os.path.join(git_root, "game/src/main/resources/")

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

    bpy.ops.export_scene.gltf(export_format='GLTF_SEPARATE', filepath=out_path, export_texture_dir=tex_dir, export_cameras=True, export_yup=False, export_apply=True, export_lights=True)

except Exception as err:
    print(err, file=sys.stderr)

