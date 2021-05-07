## Intro

[glTF](https://www.khronos.org/gltf/) is used as a format to load our scenes from. Essentially, now various things like main menu, networkable object templates, etc. etc. are set up using [Blender](https://www.blender.org/).

It is possible to model, add components, set transform types, and nest individual objects within blender, and use them as GameObjects within the game. Example building network template:

![Screenshot from 2021-03-26 14-51-19](./images/gltf/1_building_example.jpg)

Here, this building is set up the same way it was in the code to have a TransformHex, and 2 components - NetworkHexTransform, and Building:

![Screenshot from 2021-03-26 14-53-33](./images/gltf/2_building_components.jpg)

Currently, in-game shading is not representative of what the final rendering will be. Aim to make thing look right in Blender, because in the end it will look more like Blender's rendered view:

![Screenshot from 2021-03-26 15-50-29](./images/gltf/3_rendered_view.jpg)

## Setup

To modify scenes, install the dragonskulle blender addon. Go to "Edit/Preferences":

![Screenshot from 2021-03-26 14-58-22](./images/gltf/4_preferences.jpg)

Then click the "Add-ons" tab:

![Screenshot from 2021-03-26 14-59-42](./images/gltf/5_addons.png)

Click "Install...":

![Screenshot from 2021-03-26 15-00-41](./images/gltf/6_install.jpg)

Navigate to assets directory of the git repo:

![Screenshot from 2021-03-26 15-01-55](./images/gltf/7_path.jpg)

And double click the "dskulle\_addon.py". It will install it, and display the addon in the list:

![Screenshot from 2021-03-26 15-02-41](./images/gltf/8_select.jpg)

Enable it!

![Screenshot from 2021-03-26 15-04-11](./images/gltf/9_enable.jpg)

## Usage

You should now see a new tab on the right of 3D view:

![Screenshot from 2021-03-26 15-05-28](./images/gltf/10_tab.jpg)

Clicking on it will show all Game Object properties available for the last selected object:

![Screenshot from 2021-03-26 15-07-35](./images/gltf/11_selected.jpg)

Different objects have different properties:

![Screenshot from 2021-03-26 15-08-17](./images/gltf/12_other.jpg)

Transform controls whether the object will have Hex or 3D transform:

![Screenshot from 2021-03-26 15-08-48](./images/gltf/13_transform.jpg)

This will not change how the blender coordinates are interpreted - they are always 3D, but it changes what transform the objects will have in the game. Upon creating an object with Hex transform, conversion takes place to convert 3D coordinates to axial coordinates. Note that while positions are always converted fully, rotation and scale information gets truncated on TransformHex - only roll (Z axis) rotation is preserved, while the rest of the rotation, and scaling gets ignored.

Adding components is simple, just click "Add Component" button:

![Screenshot from 2021-03-26 15-17-35](./images/gltf/14_add_component.jpg)

Then you can either pick a component from the list, or enter the class name manually:

![Screenshot from 2021-03-26 15-18-00](./images/gltf/15_choices.jpg)

Class list should contain all components, you can search the list by the component's name or package:

![Screenshot from 2021-03-26 15-18-37](./images/gltf/16_list.jpg)

However, there is a chance that the component you need is not found in the list. In that case, enter the full class name, including the entire package path:

![Screenshot from 2021-03-26 15-21-38](./images/gltf/17_exact.jpg)

Once a component is added, you can control its enabled state, change its class, delete it altogether, and add a customized property (variable):

![Screenshot from 2021-03-26 15-24-56](./images/gltf/18_added.jpg)

The first three are trivial, meanwhile adding a customized property is similar to adding a component. You can either add from a parsed property list, or choose a manual name and type:

![Screenshot from 2021-03-26 15-26-54](./images/gltf/19_add_property.jpg)

Here we have an even higher probability of missing properties. In an event where a property is missing, enter it manually:

![Screenshot from 2021-03-26 15-29-36](./images/gltf/20_type_property.jpg)

Choose a matching type, currently only numbers, booleans, vectors and strings are supported. On java side, the variable either has to be a direct type (primitive, String, Vector3f), or a Sync type (SyncInt, SyncFloat, SyncVector3, etc.):

![Screenshot from 2021-03-26 15-29-12](./images/gltf/21_prop_type.jpg)

Enter its name:

![Screenshot from 2021-03-26 15-31-47](./images/gltf/22_prop_name.jpg)

And click "OK":

![Screenshot from 2021-03-26 15-32-03](./images/gltf/23_clicked_ok.jpg)

You can then set the value of the property, script it with a driver, or anything else blender allows:

![Screenshot from 2021-03-26 15-36-21](./images/gltf/24_blender_drivers.jpg)

## Exporting:

All blender scenes should be placed in assets subdirectory.

Main menu scene is inside `main_menu.blend`. Networkable templates are inside `network_templates.blend`. Shared templates are meant to be put inside `templates.blend`, and linked into other blender files (take a look at `main_menu_hexagon` inside templates and main\_menu).

Exporting on Linux can be done with the `blend2gltf.sh` script.

If running the script is not possible, open up the "Scripting" tab:

![Screenshot from 2021-03-26 15-45-19](./images/gltf/25_scripting.jpg)

Click "Open":

![Screenshot from 2021-03-26 15-46-46](./images/gltf/26_open.jpg)

Navigate to the "assets" subdirectory and open "gltf\_export.py" file:

![Screenshot from 2021-03-26 15-47-05](./images/gltf/27_path.jpg)

Run the script:

![Screenshot from 2021-03-26 15-48-09](./images/gltf/28_run.jpg)

