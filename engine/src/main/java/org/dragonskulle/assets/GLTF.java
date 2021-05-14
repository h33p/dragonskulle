/* (C) 2021 DragonSkulle */
package org.dragonskulle.assets;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.Transform;
import org.dragonskulle.components.Transform3D;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Resource;
import org.dragonskulle.core.ResourceManager;
import org.dragonskulle.core.Scene;
import org.dragonskulle.network.components.sync.ISyncVar;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.Texture;
import org.dragonskulle.renderer.TextureMapping;
import org.dragonskulle.renderer.TextureMapping.TextureFiltering;
import org.dragonskulle.renderer.TextureMapping.TextureWrapping;
import org.dragonskulle.renderer.Vertex;
import org.dragonskulle.renderer.components.Camera;
import org.dragonskulle.renderer.components.Light;
import org.dragonskulle.renderer.components.Renderable;
import org.dragonskulle.renderer.materials.IRefCountedMaterial;
import org.dragonskulle.renderer.materials.PBRMaterial;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.joml.Vector4i;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.lwjgl.system.NativeResource;

/**
 * Allows loading <a href="https://www.khronos.org/gltf/">glTF 2.0</a> assets.
 *
 * @author Aurimas Blažulionis
 *     <p>Our engine mostly follows the glTF standard, except we set up direction to be Z+, instead
 *     of Y+, which is what our engine uses. In addition, "DSKULLE_game_object" extension is
 *     utilized to store components and their properties directly on the glTF files.
 *     <p>Retrieving a {@link GLTF} resource is done through {@link GLTF#getResource} static method.
 *     The resulting resource should be freed when no longer used. It contains parsed {@link Scene}
 *     objects, with {@link GameObject} nodes, which have {@link Component} objects attached to
 *     them.
 */
@Accessors(prefix = "m")
@Log
public class GLTF implements NativeResource {

    /** Describes glTF buffer accessor. */
    private static class GLTFAccessor<T> {
        ByteBuffer mBuffer;
        int mPosition;
        int mCount;
        IGetObj<T> mGetObj;

        /** Get a specific object at byte offset. */
        private static interface IGetObj<T> {
            /**
             * Read an object from buffer.
             *
             * @param buffer buffer to read from.
             * @param position position within the buffer where the array starts.
             * @param index index of the array.
             * @return read buffer.
             */
            T get(ByteBuffer buffer, int position, int index);
        }

        /**
         * Create a glTF accessor.
         *
         * @param buffer buffer to wrap.
         * @param position position within the buffer.
         * @param count number of elements in the accessor array.
         * @param getObj interface to read elements of the array.
         */
        public GLTFAccessor(ByteBuffer buffer, int position, int count, IGetObj<T> getObj) {
            mBuffer = buffer;
            mPosition = position;
            mCount = count;
            mGetObj = getObj;
        }

        /**
         * Access an object at index.
         *
         * @param index array index to read.
         * @return object at a given index.
         */
        public T get(int index) {
            if (index >= mCount || index < 0) {
                return null;
            }
            return mGetObj.get(mBuffer, mPosition, index);
        }

        /**
         * Get an accessor from a string type.
         *
         * @param type string value of the type.
         * @param componentType type of the components within the type.
         * @param buffer buffer to read from. Must set its position to the target array start
         *     position.
         * @param count number of elements within the accessor.
         * @return a glTF accessor, if valid type is passed. {@code null} otherwise.
         */
        public static GLTFAccessor<?> fromStringType(
                String type, int componentType, ByteBuffer buffer, int count) {

            int position = buffer.position();

            IGetObj<?> getObj = null;

            switch (type) {
                case "SCALAR":
                    switch (componentType) {
                        case 5120: // BYTE
                        case 5121: // UNSIGNED_BYTE
                            getObj =
                                    (buf, pos, idx) -> {
                                        buf.rewind();
                                        buf.position(pos + idx);
                                        byte ret = buf.get();
                                        buf.rewind();
                                        return ret;
                                    };
                            break;
                        case 5122: // SHORT
                            getObj =
                                    (buf, pos, idx) -> {
                                        buf.rewind();
                                        buf.position(pos + idx * 2);
                                        short ret = buf.getShort();
                                        buf.rewind();
                                        return ret;
                                    };
                            break;
                        case 5123: // UNSIGNED_SHORT
                            getObj =
                                    (buf, pos, idx) -> {
                                        buf.rewind();
                                        buf.position(pos + idx * 2);
                                        int ret = ((int) buf.getShort()) & 0xffff;
                                        buf.rewind();
                                        return ret;
                                    };
                            break;
                        case 5124: // INT
                        case 5125: // UNSIGNED_INT
                            getObj =
                                    (buf, pos, idx) -> {
                                        buf.rewind();
                                        buf.position(pos + idx * 4);
                                        int ret = buf.getInt();
                                        buf.rewind();
                                        return ret;
                                    };
                            break;
                        case 5126: // FLOAT
                            getObj =
                                    (buf, pos, idx) -> {
                                        buf.rewind();
                                        buf.position(pos + idx * 4);
                                        float ret = buf.getFloat();
                                        buf.rewind();
                                        return ret;
                                    };
                            break;
                        default:
                            break;
                    }
                    break;
                case "VEC2":
                    switch (componentType) {
                        case 5125: // UNSIGNED_INT
                            getObj =
                                    (buf, pos, idx) -> {
                                        Vector2i ret = new Vector2i();
                                        buf.rewind();
                                        buf.position(pos + idx * 2 * 4);
                                        ret.set(buf.getInt(), buf.getInt());
                                        buf.rewind();
                                        return ret;
                                    };
                            break;
                        case 5126: // FLOAT
                            getObj =
                                    (buf, pos, idx) -> {
                                        Vector2f ret = new Vector2f();
                                        buf.rewind();
                                        buf.position(pos + idx * 2 * 4);
                                        ret.set(buf.getFloat(), buf.getFloat());
                                        buf.rewind();
                                        return ret;
                                    };
                            break;
                        default:
                            break;
                    }
                    break;
                case "VEC3":
                    switch (componentType) {
                        case 5125: // UNSIGNED_INT
                            getObj =
                                    (buf, pos, idx) -> {
                                        Vector3i ret = new Vector3i();
                                        buf.rewind();
                                        buf.position(pos + idx * 3 * 4);
                                        ret.set(buf.getInt(), buf.getInt(), buf.getInt());
                                        buf.rewind();
                                        return ret;
                                    };
                            break;
                        case 5126: // FLOAT
                            getObj =
                                    (buf, pos, idx) -> {
                                        Vector3f ret = new Vector3f();
                                        buf.rewind();
                                        buf.position(pos + idx * 3 * 4);
                                        ret.set(buf.getFloat(), buf.getFloat(), buf.getFloat());
                                        buf.rewind();
                                        return ret;
                                    };
                            break;
                        default:
                            break;
                    }
                    break;
                case "VEC4":
                    switch (componentType) {
                        case 5125: // UNSIGNED_INT
                            getObj =
                                    (buf, pos, idx) -> {
                                        Vector4i ret = new Vector4i();
                                        buf.rewind();
                                        buf.position(pos + idx * 4 * 4);
                                        ret.set(
                                                buf.getInt(),
                                                buf.getInt(),
                                                buf.getInt(),
                                                buf.getInt());
                                        buf.rewind();
                                        return ret;
                                    };
                            break;
                        case 5126: // FLOAT
                            getObj =
                                    (buf, pos, idx) -> {
                                        Vector4f ret = new Vector4f();
                                        buf.rewind();
                                        buf.position(pos + idx * 4 * 4);
                                        ret.set(
                                                buf.getFloat(),
                                                buf.getFloat(),
                                                buf.getFloat(),
                                                buf.getFloat());
                                        buf.rewind();
                                        return ret;
                                    };
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }

            return getObj == null ? null : new GLTFAccessor<>(buffer, position, count, getObj);
        }
    }

    /**
     * Describes a single glTF mesh primitive. We use a {@link Renderable} component per primitive
     */
    private static class GLTFPrimitive {
        private PBRMaterial mMaterial;
        private Mesh mMesh;
    }

    /** Describes a single glTF mesh. It is a list of {@link GLTFPrimitive} */
    private static class GLTFMesh {
        private List<GLTFPrimitive> mPrimitives = new ArrayList<>();
    }

    private List<PBRMaterial> mMaterials = new ArrayList<>();
    private List<GLTFMesh> mMeshes = new ArrayList<>();
    private int mDefaultScene = 0;
    private List<Scene> mScenes = new ArrayList<>();
    private List<Camera> mCameras = new ArrayList<>();
    private List<Light> mLights = new ArrayList<>();

    static {
        ResourceManager.registerResource(
                GLTF.class,
                (args) -> String.format("gltf/%s.gltf", args.getName()),
                (buffer, __) -> new GLTF(new String(buffer)));
    }

    /**
     * Load a GLTF resource.
     *
     * @param name name of the glTF file. gltf subdirectory will be added, alongside the .gltf
     *     extension.
     * @return GLTF resource if successfully loaded, {@code null} otherwise
     */
    public static Resource<GLTF> getResource(String name) {
        return ResourceManager.getResource(GLTF.class, name);
    }

    /**
     * Parses a floating point variable from JSON object.
     *
     * @param obj JSON to parse from
     * @param key JSON key to read
     * @param defaultValue default float value
     * @return parsed float value (defaultValue if failed to parse).
     */
    private static boolean parseBool(JSONObject obj, String key, boolean defaultValue) {
        Object val = obj.get(key);
        if (val != null) {
            return Boolean.parseBoolean(val.toString());
        }
        return defaultValue;
    }

    /**
     * Parses a floating point variable from JSON object.
     *
     * @param obj JSON to parse from
     * @param key JSON key to read
     * @param defaultValue default float value
     * @return parsed float value (defaultValue if failed to parse).
     */
    private static float parseFloat(JSONObject obj, String key, float defaultValue) {
        Object val = obj.get(key);
        if (val != null) {
            return Float.parseFloat(val.toString());
        }
        return defaultValue;
    }

    /**
     * Parses a floating point variable from JSON object.
     *
     * @param val object to parse from
     * @return parsed float value or {@code null} if failed parsing
     */
    private static Float parseFloat(Object val) {
        if (val != null) {
            return Float.parseFloat(val.toString());
        }
        return null;
    }

    /**
     * Parses a integer variable from JSON object.
     *
     * @param obj JSON to parse from.
     * @param key JSON key to read.
     * @param defaultValue default int value.
     * @return parsed int value (defaultValue if failed to parse).
     */
    private static int parseInt(JSONObject obj, String key, int defaultValue) {
        Object val = obj.get(key);
        if (val != null) {
            return Integer.parseInt(val.toString());
        }
        return defaultValue;
    }

    /**
     * Parses a integer point variable from JSON object.
     *
     * @param obj object to parse from.
     * @param key JSON key to read.
     * @return parsed int value or {@code null} if failed parsing.
     */
    private static Integer parseInt(JSONObject obj, String key) {
        Object val = obj.get(key);
        if (val != null) {
            return Integer.parseInt(val.toString());
        }
        return null;
    }

    /**
     * Parses a integer variable from a scalar.
     *
     * @param obj object to parse from.
     * @return parsed int value, or {@code 0} if failed parsing.
     */
    private static int parseIntFromScalar(Object obj) {
        if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof Long) {
            return (int) (long) (Long) obj;
        } else if (obj instanceof Short) {
            return (int) (short) (Short) obj;
        } else if (obj instanceof Byte) {
            return (int) (byte) (Byte) obj;
        } else if (obj instanceof Float) {
            return (int) (float) (Float) obj;
        } else if (obj instanceof Double) {
            return (int) (double) (Double) obj;
        }
        return 0;
    }

    /**
     * Constructor for {@link GLTF}.
     *
     * @param data JSON string data to parse.
     * @throws ParseException when parsing JSON fails.
     */
    private GLTF(String data) throws ParseException {
        JSONObject decoded = (JSONObject) JSONValue.parse(data);

        JSONArray images = (JSONArray) decoded.get("images");
        List<Resource<Texture>> loadedImages = new ArrayList<>();

        if (images != null) {
            for (Object image : images) {
                String uri = (String) ((JSONObject) image).get("uri");
                if (uri.startsWith("../textures/")) {
                    uri = uri.replaceFirst("../textures/", "");
                }
                loadedImages.add(Texture.getResource(uri));
            }
        }

        JSONArray samplers = (JSONArray) decoded.get("samplers");
        List<TextureMapping> mappings = new ArrayList<>();

        if (samplers != null) {
            for (Object obj : samplers) {
                JSONObject sampler = (JSONObject) obj;
                TextureFiltering magFilter =
                        TextureFiltering.fromGLTF(parseInt(sampler, "magFilter"));
                // TODO: Support customizable mipmap filters
                // TextureFiltering minFilter =
                // TextureFiltering.fromGLTF((Integer)sampler.get("minFilter"));
                TextureWrapping wrapU = TextureWrapping.fromGLTF(parseInt(sampler, "wrapS"));
                TextureWrapping wrapV = TextureWrapping.fromGLTF(parseInt(sampler, "wrapT"));
                TextureWrapping wrapW = TextureWrapping.CLAMP;
                mappings.add(new TextureMapping(magFilter, wrapU, wrapV, wrapW));
            }
        }

        JSONArray textures = (JSONArray) decoded.get("textures");
        List<SampledTexture> texs = new ArrayList<>();

        if (textures != null) {
            for (Object obj : textures) {
                JSONObject tex = (JSONObject) obj;
                int sampler = parseInt(tex, "sampler");
                int source = parseInt(tex, "source");
                texs.add(new SampledTexture(loadedImages.get(source), mappings.get(sampler)));
            }
        }

        JSONArray materials = (JSONArray) decoded.get("materials");

        Vector4f baseColour = new Vector4f(1f);
        Vector3f emissionColour = new Vector3f(0f);

        if (materials != null) {
            for (Object obj : materials) {
                JSONObject mat = (JSONObject) obj;
                float metallic = 1f;
                float roughness = 1f;
                baseColour.set(1f);
                emissionColour.set(0);
                SampledTexture baseSampled = null;
                SampledTexture metallicSampled = null;

                String alphaMode = (String) mat.get("alphaMode");

                JSONArray emission = (JSONArray) mat.get("emissiveFactor");

                if (emission != null) {
                    emissionColour.set(
                            parseFloat(emission.get(0)),
                            parseFloat(emission.get(1)),
                            parseFloat(emission.get(2)));
                }

                JSONObject rough = (JSONObject) mat.get("pbrMetallicRoughness");
                if (rough != null) {
                    JSONArray baseFactor = (JSONArray) rough.get("baseColorFactor");
                    if (baseFactor != null) {
                        baseColour.set(
                                parseFloat(baseFactor.get(0)),
                                parseFloat(baseFactor.get(1)),
                                parseFloat(baseFactor.get(2)),
                                parseFloat(baseFactor.get(3)));
                    }
                    JSONObject baseTex = (JSONObject) rough.get("baseColorTexture");

                    if (baseTex != null) {
                        Integer texIdx = parseInt(baseTex, "index");
                        if (texIdx != null) {
                            baseSampled = texs.get(texIdx);
                        }
                    }

                    JSONObject metalTex = (JSONObject) rough.get("metallicRoughnessTexture");

                    if (metalTex != null) {
                        Integer texIdx = parseInt(metalTex, "index");
                        if (texIdx != null) {
                            metallicSampled = texs.get(texIdx);
                        }
                    }

                    metallic = parseFloat(rough, "metallicFactor", 1f);
                    roughness = parseFloat(rough, "roughnessFactor", 1f);
                }

                JSONObject normalTex = (JSONObject) mat.get("normalTexture");
                float normal = 1.f;
                SampledTexture normalSampled = null;
                if (normalTex != null) {
                    Integer texIdx = parseInt(normalTex, "index");
                    if (texIdx != null) {
                        normalSampled = texs.get(texIdx);
                    }
                    normal = parseFloat(normalTex, "scale", 1.f);
                    // TODO: TEXCOORD
                }

                PBRMaterial pbrMat = new PBRMaterial(baseColour);

                if (alphaMode != null) {
                    switch (alphaMode) {
                        case "BLEND":
                            pbrMat.setAlphaBlend(true);
                            break;
                        default:
                            break;
                    }
                }

                if (baseSampled != null) {
                    pbrMat.setAlbedoMap(baseSampled);
                }
                if (normalSampled != null) {
                    pbrMat.setNormalMap(normalSampled);
                }
                if (metallicSampled != null) {
                    pbrMat.setMetalnessRoughnessMap(metallicSampled);
                }
                pbrMat.setMetallic(metallic);
                pbrMat.setRoughness(roughness);
                pbrMat.setNormal(normal);
                pbrMat.getEmissionColour().set(emissionColour);

                mMaterials.add(pbrMat);
            }
        }

        JSONArray buffers = (JSONArray) decoded.get("buffers");
        List<Resource<byte[]>> bufferList = new ArrayList<>();

        if (buffers != null) {
            for (Object obj : buffers) {
                JSONObject buf = (JSONObject) obj;
                bufferList.add(
                        ResourceManager.getResource(
                                byte[].class, "gltf/" + (String) buf.get("uri")));
            }
        }

        JSONArray bufferViews = (JSONArray) decoded.get("bufferViews");
        List<ByteBuffer> bufferViewList = new ArrayList<>();

        if (bufferViews != null) {
            for (Object obj : bufferViews) {
                JSONObject view = (JSONObject) obj;
                int buf = parseInt(view, "buffer");
                int len = parseInt(view, "byteLength");
                int off = parseInt(view, "byteOffset");
                ByteBuffer bBuf = ByteBuffer.wrap(bufferList.get(buf).get(), off, len);
                bBuf.order(ByteOrder.LITTLE_ENDIAN);
                bufferViewList.add(bBuf);
            }
        }

        JSONArray accessors = (JSONArray) decoded.get("accessors");
        List<GLTFAccessor<?>> accessorList = new ArrayList<>();

        if (accessors != null) {
            for (Object obj : accessors) {
                JSONObject accessor = (JSONObject) obj;
                int view = parseInt(accessor, "bufferView");
                int count = parseInt(accessor, "count");
                String type = accessor.get("type").toString();
                int componentType = parseInt(accessor, "componentType");
                accessorList.add(
                        GLTFAccessor.fromStringType(
                                type, componentType, bufferViewList.get(view), count));
            }
        }

        JSONArray meshes = (JSONArray) decoded.get("meshes");

        if (meshes != null) {
            for (Object obj : meshes) {
                JSONObject mesh = (JSONObject) obj;
                // TODO: name for mesh lookup?
                Vertex[] vertices = null;
                int[] indices = null;
                Integer matIdx = null;

                GLTFMesh outMesh = new GLTFMesh();

                JSONArray submeshes = (JSONArray) mesh.get("primitives");

                if (submeshes != null && submeshes.size() > 0) {
                    for (Object submeshObj : submeshes) {
                        JSONObject submesh = (JSONObject) submeshObj;

                        matIdx = parseInt(submesh, "material");

                        GLTFAccessor<?> indexAccessor =
                                accessorList.get(parseInt(submesh, "indices"));
                        indices = new int[indexAccessor.mCount];

                        for (int i = 0; i < indices.length; i++) {
                            indices[i] = parseIntFromScalar(indexAccessor.get(i));
                        }

                        JSONObject attributes = (JSONObject) submesh.get("attributes");
                        if (attributes != null) {
                            GLTFAccessor<?> posAccessor =
                                    accessorList.get(parseInt(attributes, "POSITION"));
                            GLTFAccessor<?> normAccessor =
                                    accessorList.get(parseInt(attributes, "NORMAL"));
                            GLTFAccessor<?> uvAccessor =
                                    accessorList.get(parseInt(attributes, "TEXCOORD_0"));
                            if (posAccessor.mCount == uvAccessor.mCount) {
                                vertices = new Vertex[posAccessor.mCount];

                                for (int i = 0; i < vertices.length; i++) {
                                    Vector3f pos = (Vector3f) posAccessor.get(i);
                                    Vector3f norm = (Vector3f) normAccessor.get(i);
                                    Vector2f uv = (Vector2f) uvAccessor.get(i);
                                    vertices[i] = new Vertex();
                                    vertices[i].getPos().set(pos);
                                    vertices[i].getNormal().set(norm);
                                    vertices[i].getUv().set(uv);
                                }
                            }
                        }

                        GLTFPrimitive outPrimitive = new GLTFPrimitive();
                        outPrimitive.mMesh = new Mesh(vertices, indices);
                        outPrimitive.mMaterial = matIdx == null ? null : mMaterials.get(matIdx);
                        outMesh.mPrimitives.add(outPrimitive);
                    }
                }

                mMeshes.add(outMesh);
            }
        }

        JSONArray cameras = (JSONArray) decoded.get("cameras");
        if (cameras != null) {
            for (Object obj : cameras) {
                JSONObject cam = (JSONObject) obj;
                String type = cam.get("type").toString();

                Camera camera = new Camera();
                // Blender cameras look down.
                camera.getViewDirection().set(0, 0, -1);
                switch (type) {
                    case "perspective":
                        {
                            JSONObject persp = (JSONObject) cam.get("perspective");
                            float fov = parseFloat(persp, "yfov", 45f);
                            float zfar = parseFloat(persp, "zfar", 100f);
                            float znear = parseFloat(persp, "znear", 0.01f);
                            camera.setProjection(Camera.Projection.PERSPECTIVE);
                            camera.setFov(fov);
                            camera.setFarPlane(zfar);
                            camera.setNearPlane(znear);
                        }
                        break;
                    case "orthographic":
                        {
                            JSONObject persp = (JSONObject) cam.get("orthographic");
                            float size = parseFloat(persp, "ymag", 10f);
                            float zfar = parseFloat(persp, "zfar", 100f);
                            float znear = parseFloat(persp, "znear", 0.01f);
                            camera.setProjection(Camera.Projection.ORTHOGRAPHIC);
                            camera.setOrthographicSize(size);
                            camera.setFarPlane(zfar);
                            camera.setNearPlane(znear);
                        }
                        break;
                    default:
                        break;
                }
                mCameras.add(camera);
            }
        }

        JSONObject extensions = (JSONObject) decoded.get("extensions");
        if (extensions != null) {
            JSONObject khrLights = (JSONObject) extensions.get("KHR_lights_punctual");
            if (khrLights != null) {
                JSONArray lights = (JSONArray) khrLights.get("lights");
                if (lights != null) {
                    for (Object obj : lights) {
                        JSONObject light = (JSONObject) obj;

                        Light addLight = new Light();

                        addLight.setIntensity(parseFloat(light, "intensity", 1f));

                        String type = (String) light.get("type");

                        switch (type) {
                            default:
                                break;
                        }

                        JSONArray col = (JSONArray) light.get("color");

                        if (col != null) {
                            addLight.getColour()
                                    .set(
                                            parseFloat(col.get(0)),
                                            parseFloat(col.get(1)),
                                            parseFloat(col.get(2)));
                        }

                        mLights.add(addLight);
                    }
                }
            }
        }

        JSONArray nodes = (JSONArray) decoded.get("nodes");

        mDefaultScene = parseInt(decoded, "scene", 0);

        JSONArray scenes = (JSONArray) decoded.get("scenes");
        if (scenes != null) {
            for (Object obj : scenes) {
                JSONObject scene = (JSONObject) obj;
                String name = scene.get("name").toString();
                Scene outScene = new Scene(name);

                JSONArray sceneNodes = (JSONArray) scene.get("nodes");
                if (sceneNodes != null) {
                    for (Object nidxObj : sceneNodes) {
                        int nodeIdx = parseIntFromScalar(nidxObj);
                        outScene.addRootObject(parseNode(nodes, nodeIdx));
                    }
                }

                mScenes.add(outScene);
            }
        }

        bufferList.stream().filter(b -> b != null).forEach(Resource::free);
        loadedImages.stream().filter(e -> e != null).forEach(Resource::free);
    }

    /**
     * Parse a game object from node.
     *
     * @param nodes nodes of objects.
     * @param idx index within the node array.
     * @return parsed game object.
     */
    public GameObject parseNode(JSONArray nodes, int idx) {
        JSONObject node = (JSONObject) nodes.get(idx);
        String name = node.get("name").toString();

        JSONArray translation = (JSONArray) node.get("translation");
        Vector3f translationVec;

        if (translation != null) {
            translationVec =
                    new Vector3f(
                            parseFloat(translation.get(0)),
                            parseFloat(translation.get(1)),
                            parseFloat(translation.get(2)));
        } else {
            translationVec = new Vector3f();
        }

        JSONArray rotation = (JSONArray) node.get("rotation");
        Quaternionf rotationQuat;

        if (rotation != null) {
            rotationQuat =
                    new Quaternionf(
                            parseFloat(rotation.get(0)),
                            parseFloat(rotation.get(1)),
                            parseFloat(rotation.get(2)),
                            parseFloat(rotation.get(3)));
        } else {
            rotationQuat = new Quaternionf();
        }

        JSONArray scale = (JSONArray) node.get("scale");
        Vector3f scaleVec;

        if (scale != null) {
            scaleVec =
                    new Vector3f(
                            parseFloat(scale.get(0)),
                            parseFloat(scale.get(1)),
                            parseFloat(scale.get(2)));
        } else {
            scaleVec = new Vector3f(1f);
        }

        JSONObject extensions = (JSONObject) node.get("extensions");
        JSONObject gameObj;
        JSONObject lights;

        if (extensions != null) {
            gameObj = (JSONObject) extensions.get("DSKULLE_game_object");
            lights = (JSONObject) extensions.get("KHR_lights_punctual");
        } else {
            gameObj = null;
            lights = null;
        }

        String transformType = gameObj != null ? (String) gameObj.get("transform") : null;

        Transform transform = parseTransform(transformType, translationVec, rotationQuat, scaleVec);

        GameObject ret =
                new GameObject(
                        name,
                        transform,
                        (handle) -> {
                            Integer mesh = parseInt(node, "mesh");
                            if (mesh != null) {

                                for (GLTFPrimitive primitive : mMeshes.get(mesh).mPrimitives) {

                                    IRefCountedMaterial mat =
                                            primitive.mMaterial == null
                                                    ? new PBRMaterial()
                                                    : primitive.mMaterial.incRefCount();

                                    Renderable rend = new Renderable(primitive.mMesh, mat);
                                    handle.addComponent(rend);
                                }
                            }

                            Integer camera = parseInt(node, "camera");
                            if (camera != null) {
                                handle.addComponent(mCameras.get(camera));
                            }

                            if (lights != null) {
                                Integer lidx = parseInt(lights, "light");
                                if (lidx != null && lidx >= 0 && mLights.size() > lidx) {
                                    handle.addComponent(mLights.get(lidx));
                                }
                            }

                            if (gameObj != null) {
                                JSONArray comps = (JSONArray) gameObj.get("components");
                                if (comps != null) {
                                    for (Object comp : comps) {
                                        parseComponent(handle, (JSONObject) comp);
                                    }
                                }
                            }

                            JSONArray children = (JSONArray) node.get("children");
                            if (children != null) {
                                for (Object cidx : children) {
                                    handle.addChild(parseNode(nodes, parseIntFromScalar(cidx)));
                                }
                            }
                        });

        return ret;
    }

    /**
     * Parse the object's transformation class and create it.
     *
     * @param name Transform class name
     * @param position 3D position
     * @param rotation 3D orientation
     * @param scale 3D scale
     * @return parsed transform. Defaults to {@code Transform3D} if it fails to parse.
     */
    private Transform parseTransform(
            String name, Vector3f position, Quaternionf rotation, Vector3f scale) {
        Class<?> type = Transform3D.class;

        if (name != null) {
            try {
                type = Class.forName(name);
            } catch (ClassNotFoundException ignored) {
            }
        }

        Transform instance;

        try {
            instance = (Transform) type.getConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            instance = new Transform3D();
        }

        instance.setLocal3DTransformation(position, rotation, scale);

        return instance;
    }

    /**
     * Parse component from JSON and add it on the game object.
     *
     * @param gameObject game object to add the component on
     * @param component JSON data describing the component
     */
    private void parseComponent(GameObject gameObject, JSONObject component) {
        String className = (String) component.get("class_name");

        if (className == null) {
            return;
        }

        try {
            Class<?> type = Class.forName(className);

            // Make sure we are parsing a component
            // Avoid adding transform as a component
            if (!Component.class.isAssignableFrom(type) || Transform.class.isAssignableFrom(type)) {
                return;
            }

            Component comp = null;

            try {
                comp = (Component) type.getConstructor().newInstance();
            } catch (Exception e) {
                log.warning("Failed to instantiate: " + className);
                return;
            }

            JSONArray properties = (JSONArray) component.get("properties");

            if (properties != null) {
                for (Object obj : properties) {
                    if (!(obj instanceof JSONObject)) {
                        continue;
                    }
                    JSONObject prop = (JSONObject) obj;
                    String name = (String) prop.get("name");
                    if (name == null) {
                        continue;
                    }
                    Object value = prop.get("value");
                    if (value == null) {
                        continue;
                    }
                    assignComponentField(comp, name, value);
                }
            }

            boolean enabled = parseBool(component, "enabled", true);

            comp.setEnabled(enabled);

            gameObject.addComponent(comp);
        } catch (ClassNotFoundException e) {
            log.warning("Class not found: " + className);
            return;
        }
    }

    /**
     * Assign value to variable by name.
     *
     * @param comp component to assign the variable to
     * @param name name of the variable
     * @param value string value of the variable
     */
    private void assignComponentField(Component comp, String name, Object value) {
        Field f;
        try {
            f = comp.getClass().getDeclaredField(name);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        f.setAccessible(true);

        try {
            writeField(comp, f, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Write a field into the object.
     *
     * <p>This method will write into primitive fields, or recurse into vectors/syncvars
     *
     * @param obj object that has the field
     * @param f field on the object
     * @param value string value of the field
     */
    private void writeField(Object obj, Field f, Object value) throws IllegalAccessException {
        Class<?> type = f.getType();

        if (type.isPrimitive()) {
            writePrimitiveField(obj, f, value);
        } else {
            // Check here whether it's a Vec, or SyncVar
            if (ISyncVar.class.isAssignableFrom(type)) {
                Field[] syncFields = type.getDeclaredFields();
                for (Field sf : syncFields) {
                    Class<?> sfType = sf.getType();
                    if (sfType.isPrimitive() || Vector3f.class.isAssignableFrom(sfType)) {
                        sf.setAccessible(true);
                        ISyncVar svar = (ISyncVar) f.get(obj);
                        writeField(svar, sf, value);
                        break;
                    }
                }
            } else if (String.class.isAssignableFrom(type)) {
                f.set(obj, value.toString());
            } else if (Vector3f.class.isAssignableFrom(type)) {
                Vector3f vec = (Vector3f) f.get(obj);
                JSONArray values = (JSONArray) value;
                vec.set(
                        parseFloat(values.get(0)),
                        parseFloat(values.get(1)),
                        parseFloat(values.get(2)));
            }
        }
    }

    /**
     * Write a primitive field into the object.
     *
     * @param obj object containing the primitive field
     * @param f the primitive field
     * @param value string value of the primitive
     */
    private void writePrimitiveField(Object obj, Field f, Object value)
            throws IllegalAccessException {
        Class<?> type = f.getType();

        if (type == int.class) {
            f.setInt(obj, Integer.parseInt(value.toString()));
        } else if (type == long.class) {
            f.setLong(obj, Long.parseLong(value.toString()));
        } else if (type == short.class) {
            f.setShort(obj, Short.parseShort(value.toString()));
        } else if (type == byte.class) {
            f.setByte(obj, Byte.parseByte(value.toString()));
        } else if (type == float.class) {
            f.setFloat(obj, Float.parseFloat(value.toString()));
        } else if (type == double.class) {
            f.setDouble(obj, Double.parseDouble(value.toString()));
        } else if (type == boolean.class) {
            f.setBoolean(obj, Boolean.parseBoolean(value.toString()));
        }
    }

    /**
     * Gets the default scene in this GLTF resource.
     *
     * @return the default scene
     */
    public Scene getDefaultScene() {
        return mScenes.get(mDefaultScene);
    }

    /**
     * Gets the scene by name.
     *
     * @param name the name of the scene
     * @return found scene, or {@code null}
     */
    public Scene getScene(String name) {
        return mScenes.stream().filter(s -> s.getName().equals(name)).findFirst().orElse(null);
    }

    @Override
    public void free() {
        for (Scene scene : mScenes) {
            for (GameObject go : scene.getGameObjects().stream().toArray(GameObject[]::new)) {
                scene.destroyRootObjectImmediate(go);
            }
        }

        for (PBRMaterial mat : mMaterials) {
            mat.free();
        }
    }
}
