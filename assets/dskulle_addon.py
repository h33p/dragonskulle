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

java_to_internal = {
    "int": "int",
    "long": "int",
    "SyncInt": "int",
    "SyncLong": "int",
    "SyncProp": "int",
    "String": "string",
    "SyncString": "string",
    "boolean": "bool",
    "SyncBool": "bool",
    "float": "float",
    "double": "float",
    "SyncFloat": "float",
    "Vector3f": "vec3",
    "SyncVector3": "vec3"
}

parsed_components = {}

# Removes all class packages, and puts spaces before capitalized words
def display_name(class_name):
    class_name = class_name.split('.')[-1]
    regexed = re.sub(r'([a-z](?=[A-Z])|[A-Z](?=[A-Z][a-z]))', r'\1 ', class_name)
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

# Retrieve a list of class variables. It by no means will be complete
# Only SyncVars and vars with @Setter will be parsed
def parse_class_variables(lines):
    parsed = []
    parsed_names = {}

    lines = re.sub(r'//.*|(\"(?:\\\\[^\"]|\\\\\"|.)*?\")|(?s)/\\*.*?\\*/', r'\1 ', lines)

    lines = lines.split('\n')

    # Let's just work off of Setters lol
    # Also, lines have to be indented with 4 spaces for it to be a class variable
    for i in range(0, len(lines)):
        line = lines[i]
        try:
            if len(line) > 5 and line[4] != ' ' and line.startswith("    "):
                if "@Setter" in line or is_sync(line) or is_supported_public(line):
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

                    if t in java_to_internal and not name in parsed_names:
                        parsed_names[name] = {}
                        parsed.append({'type_name': t, 'name': name})
        except:
            pass

    return parsed

# Is the variable a syncvar?
def is_sync(line):
    if not "Sync" in line:
        return False
    matches = ["SyncInt", "SyncBool", "SyncFloat", "SyncLong", "SyncString", "SyncVector3"]

    for m in matches:
        if m in line:
            return True

    return False

# Is the variable a public variable?
def is_supported_public(line):

    split = line.split("public")

    if len(split) < 2:
        return False

    line = split[1]

    toks = line.split()

    for i in range(0, len(toks) - 1):
        typename = toks[i]
        if typename in java_to_internal:
            name = toks[i + 1]
            if "(" in name:
                return False
            return True

    return False

class ParsedComponent:
    name = ""
    class_name = ""
    fields = []

    def __init__(self, name, class_name, fields):
        self.name = name
        self.class_name = class_name
        self.fields = fields

    def display(self):
        return self.name + " (" + self.class_name + ")"

def get_component(inp):
    if inp is None or len(inp) < 1:
        return None

    try:
        right_side = inp.split('(')[1]
        left_side = right_side.split(')')[0]
        return parsed_components[left_side]
    except:
        return None

def find_component(name):
    return None

def parse_components():
    global parsed_components
    parsed_components = {}

    cwd = os.getcwd()
    os.chdir(bpy.path.abspath("//"))
    git_root = subprocess.check_output(['git', 'rev-parse', '--show-toplevel']).decode(encoding='UTF-8').strip()
    os.chdir(cwd)

    for root in java_roots:
        path = pathlib.Path(os.path.join(git_root, root))

        for f in path.glob('**/*.java'):
            fields = parse_java_component(f)

            if fields is None:
                continue

            f = os.path.relpath(f, path)
            f = os.path.splitext(f)[0]
            f = f.replace('/', '.')

            dname = display_name(f)
            parsed_components[f] = ParsedComponent(dname, f, fields)

class ColStringProperty(bpy.types.PropertyGroup):
    name: bpy.props.StringProperty(name = "Name")

class ParsedComponents(bpy.types.PropertyGroup):
    """Contains a list of parsed components"""
    components: bpy.props.CollectionProperty(name = "components", type = ColStringProperty)

    def prepare(self, cur_name):
        global parsed_components

        if len(parsed_components) == 0:
            parse_components()

        self.components.clear()

        ret = ""

        for k in parsed_components:
            v = parsed_components[k]
            comp = self.components.add()
            comp.name = v.display()

            if k == cur_name:
                ret = v.display()

        return ret

class DSKULLE_add_component(bpy.types.Operator):
    """Add a component to this GameObject"""
    bl_idname = 'dskulle_object.add_component'
    bl_label = 'Add Component'
    bl_options = {"REGISTER", "UNDO"}

    parsed_class: bpy.props.StringProperty(name = "Class List")
    name: bpy.props.StringProperty(name = "Exact Class")
    parsed_comps: bpy.props.PointerProperty(type = ParsedComponents)

    def execute(self, context):
        parsed_comp = get_component(self.parsed_class)

        if parsed_comp is not None:
            comp = context.object.dskulle_object.components.add()
            comp.class_name = parsed_comp.class_name
        elif self.name != "":
            comp = context.object.dskulle_object.components.add()
            comp.class_name = self.name

        return {"FINISHED"}

    def invoke(self, context, event):
        self.parsed_class = self.parsed_comps.prepare("")
        self.name = ""
        return context.window_manager.invoke_props_dialog(self)

    def draw(self, context):
        layout = self.layout
        layout.prop_search(self, "parsed_class", self.parsed_comps, "components")
        comp = get_component(self.parsed_class)
        if comp is not None:
            self.name = comp.class_name
            layout.label(text = "Exact Class: " + self.name)
        else:
            layout.prop(self, "name", expand = True)

class DSKULLE_change_component_name(bpy.types.Operator):
    """Change the class name of the component"""
    bl_idname = 'dskulle_object.change_component_name'
    bl_label = ''
    bl_options = {"REGISTER", "UNDO"}

    id: bpy.props.IntProperty(default=-1)
    parsed_class: bpy.props.StringProperty(name = "Class List")
    name: bpy.props.StringProperty(name = "Exact Class")
    parsed_comps: bpy.props.PointerProperty(type = ParsedComponents)

    def execute(self, context):
        comp = context.object.dskulle_object.components[self.id]
        parsed_comp = get_component(self.parsed_class)

        if parsed_comp is not None:
            comp.class_name = parsed_comp.class_name
        elif self.name != "":
            comp.class_name = self.name

        return {"FINISHED"}

    def invoke(self, context, event):
        self.parsed_class = self.parsed_comps.prepare(context.object.dskulle_object.components[self.id].class_name)
        self.name = context.object.dskulle_object.components[self.id].class_name
        return context.window_manager.invoke_props_dialog(self)

    def draw(self, context):
        layout = self.layout
        layout.prop_search(self, "parsed_class", self.parsed_comps, "components")
        comp = get_component(self.parsed_class)
        if comp is not None:
            self.name = comp.class_name
            layout.label(text = "Exact Class: " + self.name)
        else:
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
    """Add a customized property to the component"""
    bl_idname = 'dskulle_object.add_component_property'
    bl_label = 'Customize Property'
    bl_options = {"REGISTER", "UNDO"}

    def chosen_var_callback(self, context):

        if len(parsed_components) == 0:
            parse_components()

        comp = context.object.dskulle_object.components[self.id]

        if not comp.class_name in parsed_components:
            return []

        parsed_comp = parsed_components[comp.class_name]

        cur_props = []

        for p in comp.properties:
            cur_props.append(p.name)

        ret = []

        for f in parsed_comp.fields:
            if not f['name'] in cur_props:
                add = f['type_name'] + " " + f['name']
                ret.append((add, f['name'], add))

        return ret

    id: bpy.props.IntProperty(default=-1)
    choose_type: bpy.props.EnumProperty(name = "Choose Type", items = (('LIST', 'List', 'Choose properties from parsed list'), ('TYPE', 'Type', 'Type in exact property')))
    chosen_var: bpy.props.EnumProperty(name = "Name", items = chosen_var_callback)
    var_type: bpy.props.EnumProperty(name = "Type", items = (('int','int',''),('boolean','bool',''),('float','float',''),('Vector3f','vec3',''),('String','string','')))
    var_name: bpy.props.StringProperty(name = "Name")

    def execute(self, context):
        if self.choose_type == 'LIST':
            split = self.chosen_var.split()
            if len(split) > 1:
                prop = context.object.dskulle_object.components[self.id].properties.add()
                prop.type_name = split[0]
                prop.name = split[1]
        elif self.choose_type == 'TYPE':
            prop = context.object.dskulle_object.components[self.id].properties.add()
            prop.type_name = self.var_type
            prop.name = self.var_name
            prop.obj = context.object
        return {"FINISHED"}

    def invoke(self, context, event):
        self.choose_type = 'LIST'
        return context.window_manager.invoke_props_dialog(self)

    def draw(self, context):
        layout = self.layout

        layout.prop(self, "choose_type", expand = True)

        box = layout.box()

        if self.choose_type == "LIST":
            box.prop(self, "chosen_var")
        else:
            box.prop(self, "var_type")
            box.prop(self, "var_name")

class ComponentProperty(bpy.types.PropertyGroup):
    name: bpy.props.StringProperty(name = "Name")
    type_name: bpy.props.StringProperty(name = "Type")
    string_prop: bpy.props.StringProperty(name = "Value")
    bool_prop: bpy.props.BoolProperty(name = "Value")
    int_prop: bpy.props.IntProperty(name = "Value")
    float_prop: bpy.props.FloatProperty(name = "Value")
    float_prop1: bpy.props.FloatProperty(name = "X")
    float_prop2: bpy.props.FloatProperty(name = "Y")
    float_prop3: bpy.props.FloatProperty(name = "Z")
    obj: bpy.props.PointerProperty(type = bpy.types.Object)

    def java_to_internal_type(self):
        if self.type_name in java_to_internal:
            return java_to_internal[self.type_name]
        return "unknown"

    def get_properties(self):
        internal = self.java_to_internal_type()
        if internal == "int":
            return ["int_prop"]
        elif internal == "string":
            return ["string_prop"]
        elif internal == "bool":
            return ["bool_prop"]
        elif internal == "float":
            return ["float_prop"]
        elif internal == "vec3":
            return ["float_prop1", "float_prop2", "float_prop3"]
        return []

    def serialize(self):
        internal = self.java_to_internal_type()
        ret = None
        if internal == "int":
            ret = self.int_prop
        elif internal == "string":
            ret = self.string_prop
        elif internal == "bool":
            ret = self.bool_prop
        elif internal == "float":
            ret = self.float_prop
        elif internal == "vec3":
            ret = [self.float_prop1, self.float_prop2, self.float_prop3]

        if ret is None:
            return {}

        return {"name": self.name, "value": ret}


class Component(bpy.types.PropertyGroup):
    enabled: bpy.props.BoolProperty(name = "Enable", default = True, description = "Controls whether the component is enabled")
    class_name: bpy.props.StringProperty(name = "Class Name")
    properties: bpy.props.CollectionProperty(name = "Properties", type = ComponentProperty)

    def serialize(self):
        properties = []

        for p in self.properties:
            properties.append(p.serialize())

        return {"class_name": self.class_name, "properties": properties}

class GameObject(bpy.types.PropertyGroup):
    transform_type: bpy.props.EnumProperty(name = "Transform", items = (('org.dragonskulle.components.Transform3D', '3D', 'Use Transform3D'), ('org.dragonskulle.components.TransformHex', 'Hex', 'Use TransformHex')))
    components: bpy.props.CollectionProperty(name = "Components", type = Component)

    def serialize(self, obj):
        components = []

        for c in self.components:
            components.append(c.serialize())

        return { "transform": self.transform_type, "components": components}

class DSKULLE_PT_transform(bpy.types.Panel):
    bl_idname = "DSKULLE_PT_transform"
    bl_category = "Game Object"
    bl_label = "Transform"
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"

    @classmethod
    def poll(cls, context):
        return context.active_object is not None and context.active_object.dskulle_object is not None

    def draw(self, context):
        obj = context.active_object

        self.layout.prop(obj.dskulle_object, "transform_type")

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
            row = box.row()
            row.prop(component, "enabled", text = display_name(component.class_name))
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
            extension = blender_object.dskulle_object.serialize(blender_object),
            required=extension_is_required
        )

register_classes = [
    ColStringProperty,
    ParsedComponents,
    ComponentExportProperties,
    DSKULLE_PT_export,
    DSKULLE_PT_transform,
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

def unregister():
    del bpy.types.Scene.ComponentExportProperties
    del bpy.types.Object.dskulle_object
    unreg()


