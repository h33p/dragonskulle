# Exporter based on https://github.com/KhronosGroup/glTF-Blender-IO/blob/master/example-addons/example_gltf_extension/__init__.py

import bpy, re, os, pathlib, subprocess, traceback

bl_info = {
    'name': 'DragonSkulle engine component setup',
    'blender': (2, 80, 0),
    'category': 'Object'
}

glTF_extension_name = "DSKULLE_game_object"

extension_is_required = False

java_roots = [
    "engine/src/main/java/",
    "game/src/main/java/",
];

parsed_components = []

def display_name(className):
    className = className.split('.')[-1]
    regexed = re.sub(r'([a-z](?=[A-Z])|[A-Z](?=[A-Z][a-z]))', r'\1 ', className)
    return regexed

# Roughly parses a java class, we use basic heuristics for this
def parse_java_component(filepath):
    try:
        with open(filepath, 'r') as f:
            output = f.read()

        target_matches = ["extends Component", "void onDestroy()", "implements IOnAwake", "implements IOnStart", "implements IFrameUpdate", "implements ILateFrameUpdate", "extends NetworkableComponent", "extends Renderable"]

        # Transform is a special case, it should never be added as a component
        negative_matches = ["extends Transform", "abstract class"]

        match = False

        for m in target_matches:
            if m in output:
                match = True
                break

        if match:
            for m in negative_matches:
                if m in output:
                    match = False
                    break

        if match:
            return parse_class_variables(output)
        return None
    except:
        return None

def parse_class_variables(lines):
    parsed = []

    lines = re.sub(r'//.*|(\"(?:\\\\[^\"]|\\\\\"|.)*?\")|(?s)/\\*.*?\\*/', r'\1 ', lines)

    lines = lines.split('\n')

    # Let's just work off of Setters lol
    # Also, lines have to be indented with 4 spaces for it to be a class variable
    for i in range(0, len(lines)):
        line = lines[i]
        try:
            if len(line) > 5 and line[4] != ' ' and line.startswith("    "):
                if "@Setter" in line or is_sync(line):
                    # Join 2 lines for simplicity
                    # But also, ignore blank lines
                    joined = line.strip()
                    for u in range(1, 5):
                        if i + u >= len(lines):
                            break
                        if lines[i + u].strip() != "":
                            joined += " " + lines[i + u].strip()
                            break

                    joined = joined.replace("private", "public")
                    joined = joined.replace("protected", "public")

                    if "public" in joined:
                        joined = joined.split("public")[1]

                    joined = joined.split(";")[0]
                    joined = joined.split("=")[0]

                    if "static" in joined:
                        continue

                    split = joined.split();

                    name = split[-1]
                    t = split[-2]

                    parsed.append({'typeName': t, 'name': name})
        except:
            pass

    return parsed

def is_sync(line):
    if not "Sync" in line:
        return False
    matches = ["SyncInt", "SyncBool", "SyncFloat", "SyncLong", "SyncString", "SyncVector3"]

    for m in matches:
        if m in line:
            return True

    return False

class ParsedComponent:
    name = ""
    className = ""
    fields = []

    def __init__(self, name, className, fields):
        self.name = name
        self.className = className
        self.fields = fields

        print(name)
        print(className)
        print(fields)

def get_component(inp):
    if inp is None or len(inp) < 1:
        return None

    try:
        idx = int(inp.split()[0])
        return parsed_components[idx]
    except:
        return None

def find_component(name):
    return None

class ColStringProperty(bpy.types.PropertyGroup):
    name: bpy.props.StringProperty(name = "Name")

class AddComponentProperty(bpy.types.PropertyGroup):
    properties: bpy.props.CollectionProperty(name = "attributes", type = ColStringProperty)

    def prepare(self, comp_name):
        self.properties.clear()

        comp = find_component(comp_name)

        if comp is None:
            return

class DSKULLE_parsed_components(bpy.types.PropertyGroup):
    components: bpy.props.CollectionProperty(name = "components", type = ColStringProperty)

    def prepare(self):
        global parsed_components

        if len(parsed_components) == 0 or len(self.components) == 0:
            self.components.clear()
            parsed_components = []

            git_root = subprocess.check_output(['git', 'rev-parse', '--show-toplevel']).decode(encoding='UTF-8').strip()

            cnt = 0
            for root in java_roots:
                path = pathlib.Path(os.path.join(git_root, root))
                print("Scanning " + str(path))
                for f in path.glob('**/*.java'):

                    fields = parse_java_component(f)

                    if fields is None:
                        continue

                    f = os.path.relpath(f, root)
                    f = os.path.splitext(f)[0]
                    f = f.replace('/', '.')

                    dname = display_name(f)

                    comp = self.components.add()
                    comp.name = str(cnt) + " " + dname
                    parsed_components.append(ParsedComponent(dname, f, fields))
                    cnt += 1

parsed_classes = None

class DSKULLE_add_component(bpy.types.Operator):
    """Add a component to this GameObject"""
    bl_idname = 'dskulle_object.add_component'
    bl_label = 'Add Component'
    bl_options = {"REGISTER", "UNDO"}

    name: bpy.props.StringProperty(name = "Class")

    def execute(self, context):
        if self.name == "":
            pass
        else:
            comp = context.object.dskulle_object.components.add()
            comp.className = self.name
        return {"FINISHED"}

    def invoke(self, context, event):
        return context.window_manager.invoke_props_dialog(self)

    def draw(self, context):
        layout = self.layout
        layout.prop(self, "name", expand = True)

class DSKULLE_change_component_name(bpy.types.Operator):
    """Change the class name of the component"""
    bl_idname = 'dskulle_object.change_component_name'
    bl_label = ''
    bl_options = {"REGISTER", "UNDO"}

    id: bpy.props.IntProperty(default=-1)
    parsedClass: bpy.props.StringProperty(name = "Class List")
    name: bpy.props.StringProperty(name = "Exact Class")
    parsed_comps: bpy.props.PointerProperty(type = DSKULLE_parsed_components)

    def execute(self, context):
        comp = context.object.dskulle_object.components[self.id]
        parsed_comp = get_component(self.parsedClass)

        if self.name != "" and self.name != comp.className:
            comp.className = self.name
        elif parsed_comp is not None:
            comp.className = parsed_comp.className

        return {"FINISHED"}

    def invoke(self, context, event):
        self.parsed_comps.prepare()
        self.name = context.object.dskulle_object.components[self.id].className
        return context.window_manager.invoke_props_dialog(self)

    def draw(self, context):
        layout = self.layout
        layout.prop_search(self, "parsedClass", self.parsed_comps, "components")
        layout.prop(self, "name", expand = True)

class DSKULLE_remove_component(bpy.types.Operator):
    """Remove this component"""
    bl_idname = 'dskulle_object.remove_component'
    bl_label = ''
    bl_options = {"REGISTER", "UNDO"}

    id: bpy.props.IntProperty(default=-1)

    def execute(self, context):
        context.object.dskulle_object.components.remove(self.id)
        return {"FINISHED"}

class DSKULLE_remove_component_property(bpy.types.Operator):
    """Remove this property"""
    bl_idname = 'dskulle_object.remove_component_property'
    bl_label = 'Remove Property'
    bl_options = {"REGISTER", "UNDO"}

    cid: bpy.props.IntProperty(default=-1)
    pid: bpy.props.IntProperty(default=-1)

    def execute(self, context):
        prop = context.object.dskulle_object.components[self.cid].properties.remove(self.pid)
        return {"FINISHED"}

class DSKULLE_add_component_property(bpy.types.Operator):
    """Add a property to the component"""
    bl_idname = 'dskulle_object.add_component_property'
    bl_label = 'Add Property'
    bl_options = {"REGISTER", "UNDO"}

    id: bpy.props.IntProperty(default=-1)
    varType: bpy.props.EnumProperty(name = "Type", items = (('int','int',''),('boolean','bool',''),('float','float',''),('Vector3f','vec3',''),('String','string','')))
    varName: bpy.props.StringProperty(name = "Name")

    def execute(self, context):
        prop = context.object.dskulle_object.components[self.id].properties.add()
        prop.typeName = self.varType
        prop.name = self.varName
        return {"FINISHED"}

    def invoke(self, context, event):
        return context.window_manager.invoke_props_dialog(self)

    def draw(self, context):
        layout = self.layout
        layout.prop(self, "varType", expand = True)
        layout.prop(self, "varName", expand = True)

class ComponentProperty(bpy.types.PropertyGroup):
    name: bpy.props.StringProperty(name = "Name")
    typeName: bpy.props.StringProperty(name = "Type")
    stringProp: bpy.props.StringProperty(name = "Value")
    boolProp: bpy.props.BoolProperty(name = "Value")
    intProp: bpy.props.IntProperty(name = "Value")
    floatProp: bpy.props.FloatProperty(name = "Value")
    floatProp1: bpy.props.FloatProperty(name = "X")
    floatProp2: bpy.props.FloatProperty(name = "Y")
    floatProp3: bpy.props.FloatProperty(name = "Z")

    def java_to_internal_type(self):
        if self.typeName == "int" or self.typeName == "long" or self.typeName == "SyncInt" or self.typeName == "SyncLong" or self.typeName == "SyncProp":
            return "int"
        elif self.typeName == "String" or self.typeName == "SyncString":
            return "string"
        elif self.typeName == "boolean" or self.typeName == "SyncBool":
            return "bool"
        elif self.typeName == "float" or self.typeName == "double" or self.typeName == "SyncFloat":
            return "float"
        elif self.typeName == "Vector3f" or self.typeName == "SyncVector3":
            return "vec3"
        return "unknown"

    def get_properties(self):
        internal = self.java_to_internal_type()
        if internal == "int":
            return ["intProp"]
        elif internal == "string":
            return ["stringProp"]
        elif internal == "bool":
            return ["boolProp"]
        elif internal == "float":
            return ["floatProp"]
        elif internal == "vec3":
            return ["floatProp1", "floatProp2", "floatProp3"]
        return []

    def serialize(self):
        internal = self.java_to_internal_type()
        ret = None
        if internal == "int":
            ret = self.intProp
        elif internal == "string":
            ret = self.stringProp
        elif internal == "bool":
            ret = self.boolProp
        elif internal == "float":
            ret = self.floatProp
        elif internal == "vec3":
            ret = [self.floatProp1, self.floatProp2, self.floatProp3]

        if ret is None:
            return {}

        return {"name": self.name, "value": ret}


class Component(bpy.types.PropertyGroup):
    enabled: bpy.props.BoolProperty(name = "Enable", description = "Controls whether the component is enabled")
    className: bpy.props.StringProperty(name = "Class Name")
    properties: bpy.props.CollectionProperty(name = "Properties", type = ComponentProperty)

    def serialize(self):
        properties = []

        for p in self.properties:
            properties.append(p.serialize())

        return {"className": self.className, "properties": properties}

class GameObject(bpy.types.PropertyGroup):
    name: bpy.props.StringProperty(name = "EPIC")
    components: bpy.props.CollectionProperty(name = "Components", type = Component)

    def serialize(self):
        components = []

        for c in self.components:
            components.append(c.serialize())

        return {"components": components}

class DSKULLE_PT_components(bpy.types.Panel):
    bl_idname = "DSKULLE_PT_components"
    bl_category = "Game Object"
    bl_label = "Components"
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"

    @classmethod
    def poll(cls, context):
        return context.active_object is not None and context.active_object.dskulle_object is not None

    def draw(self, context):
        obj = context.active_object

        for i, component in enumerate(obj.dskulle_object.components):
            box = self.layout.box()
            row = box.row()#.split(factor = 0.8)
            row.prop(component, "enabled", text = display_name(component.className))
            cn = row.operator(DSKULLE_change_component_name.bl_idname, icon = 'TEXT')
            cn.id = i
            rm = row.operator(DSKULLE_remove_component.bl_idname, icon = 'TRASH')
            rm.id = i
            box.separator()

            for o, prop in enumerate(component.properties):
                row = box.row()
                row.label(text=prop.name)
                col = row.column()
                for draw_prop in prop.get_properties():
                    col.prop(prop, draw_prop)

                dp = row.operator(DSKULLE_remove_component_property.bl_idname, text="", icon = 'REMOVE')
                dp.cid = i
                dp.pid = o

            ap = box.operator(DSKULLE_add_component_property.bl_idname);
            ap.id = i

        self.layout.operator(DSKULLE_add_component.bl_idname);

class ComponentExportProperties(bpy.types.PropertyGroup):
    enabled: bpy.props.BoolProperty(
        name=bl_info["name"],
        description='Include this extension in the exported glTF file.',
        default=True
    )

class DSKULLE_PT_export(bpy.types.Panel):
    bl_space_type = 'FILE_BROWSER'
    bl_region_type = 'TOOL_PROPS'
    bl_label = "Enabled"
    bl_parent_id = "GLTF_PT_export_user_extensions"
    bl_options = {'DEFAULT_CLOSED'}

    @classmethod
    def poll(cls, context):
        sfile = context.space_data
        operator = sfile.active_operator
        return operator.bl_idname == "EXPORT_SCENE_OT_gltf"

    def draw_header(self, context):
        props = bpy.context.scene.ExampleExtensionProperties
        self.layout.prop(props, 'enabled')

    def draw(self, context):
        layout = self.layout
        layout.use_property_split = True
        layout.use_property_decorate = False  # No animation.

        props = bpy.context.scene.ComponentExportProperties
        layout.active = props.enabled

        box = layout.box()
        box.label(text=glTF_extension_name)


class glTF2ExportUserExtension:
    def __init__(self):
        from io_scene_gltf2.io.com.gltf2_io_extensions import Extension
        self.Extension = Extension
        self.properties = bpy.context.scene.ComponentExportProperties

    def gather_node_hook(self, gltf2_node, blender_object, export_settings):
        if not self.properties.enabled:
            return

        if blender_object.dskulle_object is None:
            return

        if gltf2_node.extensions is None:
            gltf2_node.extensions = {}

        gltf2_node.extensions[glTF_extension_name] = self.Extension(
            name=glTF_extension_name,
            extension = blender_object.dskulle_object.serialize(),
            required=extension_is_required
        )

register_classes = [
    ColStringProperty,
    DSKULLE_parsed_components,
    ComponentExportProperties,
    DSKULLE_PT_export,
    DSKULLE_PT_components,
    DSKULLE_add_component,
    DSKULLE_change_component_name,
    DSKULLE_remove_component,
    DSKULLE_add_component_property,
    DSKULLE_remove_component_property,
    ComponentProperty,
    Component,
    GameObject,
]

reg, unreg = bpy.utils.register_classes_factory(register_classes)

def register():
    reg()
    bpy.types.Object.dskulle_object = bpy.props.PointerProperty(type = GameObject);
    bpy.types.Scene.ComponentExportProperties = bpy.props.PointerProperty(type = ComponentExportProperties)
    bpy.types.Scene.DSKULLE_parsed_components = bpy.props.PointerProperty(type = DSKULLE_parsed_components)

def unregister():
    del bpy.types.Scene.DSKULLE_parsed_components
    del bpy.types.Scene.ComponentExportProperties
    del bpy.types.Object.dskulle_object
    unreg()


