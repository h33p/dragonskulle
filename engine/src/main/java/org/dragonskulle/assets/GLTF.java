/* (C) 2021 DragonSkulle */
package org.dragonskulle.assets;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.Accessors;
import org.dragonskulle.components.*;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Resource;
import org.dragonskulle.core.ResourceManager;
import org.dragonskulle.core.Scene;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.Texture;
import org.dragonskulle.renderer.TextureMapping;
import org.dragonskulle.renderer.TextureMapping.TextureFiltering;
import org.dragonskulle.renderer.TextureMapping.TextureWrapping;
import org.dragonskulle.renderer.Vertex;
import org.dragonskulle.renderer.components.*;
import org.dragonskulle.renderer.materials.PBRMaterial;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.joml.Vector4i;
import org.json.simple.*;
import org.json.simple.parser.ParseException;
import org.lwjgl.system.NativeResource;

/**
 * Class describing a renderable UI object
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class GLTF implements NativeResource {

    private static class GLTFAccessor<T> {
        ByteBuffer mBuffer;
        int mCount;
        IGetObj<T> mGetObj;

        private static interface IGetObj<T> {
            T get(ByteBuffer buffer, int index);
        }

        public GLTFAccessor(ByteBuffer buffer, int count, IGetObj<T> getObj) {
            mBuffer = buffer;
            mCount = count;
            mGetObj = getObj;
        }

        public T get(int index) {
            if (index >= mCount || index < 0) return null;
            return mGetObj.get(mBuffer, index);
        }

        public static GLTFAccessor<?> fromStringType(
                String type, int componentType, ByteBuffer buffer, int count) {
            IGetObj<?> getObj = null;

            switch (type) {
                case "SCALAR":
                    switch (componentType) {
                        case 5120: // BYTE
                        case 5121: // UNSIGNED_BYTE
                            getObj =
                                    (buf, idx) -> {
                                        buf.rewind();
                                        buf.position(idx);
                                        byte ret = buf.get();
                                        buf.rewind();
                                        return ret;
                                    };
                            break;
                        case 5122: // SHORT
                        case 5123: // UNSIGNED_SHORT
                            getObj =
                                    (buf, idx) -> {
                                        buf.rewind();
                                        buf.position(idx * 2);
                                        short ret = buf.getShort();
                                        buf.rewind();
                                        return ret;
                                    };
                            break;
                        case 5124: // INT
                        case 5125: // UNSIGNED_INT
                            getObj =
                                    (buf, idx) -> {
                                        buf.rewind();
                                        buf.position(idx * 4);
                                        int ret = buf.getInt();
                                        buf.rewind();
                                        return ret;
                                    };
                            break;
                        case 5126: // FLOAT
                            getObj =
                                    (buf, idx) -> {
                                        buf.rewind();
                                        buf.position(idx * 4);
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
                                    (buf, idx) -> {
                                        Vector2i ret = new Vector2i();
                                        buf.rewind();
                                        buf.position(idx * 2 * 4);
                                        ret.set(buf.getInt(), buf.getInt());
                                        buf.rewind();
                                        return ret;
                                    };
                            break;
                        case 5126: // FLOAT
                            getObj =
                                    (buf, idx) -> {
                                        Vector2f ret = new Vector2f();
                                        buf.rewind();
                                        buf.position(idx * 2 * 4);
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
                                    (buf, idx) -> {
                                        Vector3i ret = new Vector3i();
                                        buf.rewind();
                                        buf.position(idx * 3 * 4);
                                        ret.set(buf.getInt(), buf.getInt(), buf.getInt());
                                        buf.rewind();
                                        return ret;
                                    };
                            break;
                        case 5126: // FLOAT
                            getObj =
                                    (buf, idx) -> {
                                        Vector3f ret = new Vector3f();
                                        buf.rewind();
                                        buf.position(idx * 3 * 4);
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
                                    (buf, idx) -> {
                                        Vector4i ret = new Vector4i();
                                        buf.rewind();
                                        buf.position(idx * 4 * 4);
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
                                    (buf, idx) -> {
                                        Vector4f ret = new Vector4f();
                                        buf.rewind();
                                        buf.position(idx * 4 * 4);
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

            return getObj == null ? null : new GLTFAccessor<>(buffer, count, getObj);
        }
    }

    private List<PBRMaterial> mMaterials = new ArrayList<>();
    private List<Mesh> mMeshes = new ArrayList<>();
    private List<Integer> mMatIndices = new ArrayList<>();
    private int mDefaultScene = 0;
    private List<Scene> mScenes = new ArrayList<>();
    private List<Camera> mCameras = new ArrayList<>();

    private static float parseFloat(JSONObject obj, String key, float defaultValue) {
        Object val = obj.get(key);
        if (val != null) return Float.parseFloat(val.toString());
        return defaultValue;
    }

    private static Float parseFloat(JSONObject obj, String key) {
        Object val = obj.get(key);
        if (val != null) return Float.parseFloat(val.toString());
        return null;
    }

    private static Float parseFloat(Object val) {
        if (val != null) return Float.parseFloat(val.toString());
        return null;
    }

    private static int parseInt(JSONObject obj, String key, int defaultValue) {
        Object val = obj.get(key);
        if (val != null) return Integer.parseInt(val.toString());
        return defaultValue;
    }

    private static Integer parseInt(JSONObject obj, String key) {
        Object val = obj.get(key);
        if (val != null) return Integer.parseInt(val.toString());
        return null;
    }

    private static int parseIntFromScalar(Object obj) {
        if (obj instanceof Integer) return (Integer) obj;
        else if (obj instanceof Long) return (int) (long) (Long) obj;
        else if (obj instanceof Short) return (int) (short) (Short) obj;
        else if (obj instanceof Byte) return (int) (byte) (Byte) obj;
        else if (obj instanceof Float) return (int) (float) (Float) obj;
        else if (obj instanceof Double) return (int) (double) (Double) obj;
        return 0;
    }

    private GLTF(String data) throws ParseException {
        JSONObject decoded = (JSONObject) JSONValue.parse(data);

        JSONArray images = (JSONArray) decoded.get("images");
        List<Resource<Texture>> loadedImages = new ArrayList<>();

        if (images != null) {
            for (Object image : images) {
                String uri = (String) ((JSONObject) image).get("uri");
                System.out.println(uri);
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

        Vector4f baseColor = new Vector4f(1f);

        if (materials != null) {
            for (Object obj : materials) {
                JSONObject mat = (JSONObject) obj;
                float metallic = 1f;
                float roughness = 1f;
                baseColor.set(1f);
                SampledTexture baseSampled = null;

                JSONObject rough = (JSONObject) mat.get("pbrMetallicRoughness");
                if (rough != null) {
                    JSONArray baseFactor = (JSONArray) rough.get("baseColorFactor");
                    if (baseFactor != null)
                        baseColor.set(
                                parseFloat(baseFactor.get(0)),
                                parseFloat(baseFactor.get(1)),
                                parseFloat(baseFactor.get(2)),
                                parseFloat(baseFactor.get(3)));
                    JSONObject baseTex = (JSONObject) rough.get("baseColorTexture");

                    if (baseTex != null) {
                        Integer texIdx = parseInt(baseTex, "index");
                        if (texIdx != null) baseSampled = texs.get(texIdx);
                    }

                    metallic = parseFloat(rough, "metallicFactor", 1f);
                    roughness = parseFloat(rough, "roughnessFactor", 1f);
                }

                JSONObject normalTex = (JSONObject) mat.get("normalTexture");
                float normal = 1.f;
                SampledTexture normalSampled = null;
                if (normalTex != null) {
                    Integer texIdx = parseInt(normalTex, "index");
                    if (texIdx != null) normalSampled = texs.get(texIdx);
                    normal = parseFloat(normalTex, "scale", 1.f);
                    // TODO: TEXCOORD
                }

                PBRMaterial pbrMat = new PBRMaterial(baseColor);
                if (baseSampled != null) pbrMat.getFragmentTextures()[0] = baseSampled.clone();
                if (normalSampled != null) pbrMat.getFragmentTextures()[1] = normalSampled.clone();
                pbrMat.setMetallic(metallic);
                pbrMat.setRoughness(roughness);
                pbrMat.setNormal(normal);

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
                                byte[].class, (b) -> b, "gltf/" + (String) buf.get("uri")));
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
                bufferViewList.add(ByteBuffer.wrap(bufferList.get(buf).get(), off, len));
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
                JSONArray submeshes = (JSONArray) mesh.get("primitives");

                if (submeshes != null && submeshes.size() > 0) {
                    JSONObject submesh = (JSONObject) submeshes.get(0);

                    matIdx = parseInt(submesh, "material");

                    GLTFAccessor<?> indexAccessor = accessorList.get(parseInt(submesh, "indices"));
                    indices = new int[indexAccessor.mCount];

                    for (int i = 0; i < indices.length; i++)
                        indices[i] = parseIntFromScalar(indexAccessor.get(i));

                    JSONObject attributes = (JSONObject) submesh.get("attributes");
                    if (attributes != null) {
                        GLTFAccessor<?> posAccessor =
                                accessorList.get(parseInt(attributes, "POSITION"));
                        GLTFAccessor<?> uvAccessor =
                                accessorList.get(parseInt(attributes, "TEXCOORD_0"));
                        if (posAccessor.mCount == uvAccessor.mCount) {
                            vertices = new Vertex[posAccessor.mCount];

                            for (int i = 0; i < vertices.length; i++) {
                                Vector3f pos = (Vector3f) posAccessor.get(i);
                                Vector2f uv = (Vector2f) uvAccessor.get(i);
                                vertices[i] = new Vertex();
                                vertices[i].setPos(pos);
                                vertices[i].setUv(uv);
                            }
                        }
                    }
                }

                mMatIndices.add(matIdx);

                if (vertices != null && indices != null) mMeshes.add(new Mesh(vertices, indices));
                else mMeshes.add(null);
            }
        }

        JSONArray cameras = (JSONArray) decoded.get("cameras");
        if (cameras != null) {
            for (Object obj : cameras) {
                JSONObject cam = (JSONObject) obj;
                String type = cam.get("type").toString();

                Camera camera = new Camera();
                switch (type) {
                    case "perspective":
                        {
                            JSONObject persp = (JSONObject) cam.get("perspective");
                            float fov = parseFloat(persp, "yfov", 45f);
                            float zfar = parseFloat(persp, "zfar", 100f);
                            float znear = parseFloat(persp, "znear", 0.01f);
                            camera.projection = Camera.Projection.PERSPECTIVE;
                            camera.fov = fov;
                            camera.farPlane = zfar;
                            camera.nearPlane = znear;
                        }
                        break;
                    case "orthographic":
                        {
                            JSONObject persp = (JSONObject) cam.get("orthographic");
                            float size = parseFloat(persp, "ymag", 10f);
                            float zfar = parseFloat(persp, "zfar", 100f);
                            float znear = parseFloat(persp, "znear", 0.01f);
                            camera.projection = Camera.Projection.ORTHOGRAPHIC;
                            camera.orthographicSize = size;
                            camera.farPlane = zfar;
                            camera.nearPlane = znear;
                        }
                        break;
                    default:
                        break;
                }
                mCameras.add(camera);
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
                for (Object nidxObj : sceneNodes) {
                    int nodeIdx = parseIntFromScalar(nidxObj);
                    outScene.addRootObject(parseNode(nodes, nodeIdx));
                }

                mScenes.add(outScene);
            }
        }

        bufferList.stream().filter(b -> b != null).forEach(Resource::free);
        loadedImages.stream().filter(e -> e != null).forEach(Resource::free);
    }

    public GameObject parseNode(JSONArray nodes, int idx) throws ParseException {
        JSONObject node = (JSONObject) nodes.get(idx);
        String name = node.get("name").toString();

        // We said no exceptions, but this whole thing is between an exception
        ParseException[] exception = {null};

        GameObject ret =
                new GameObject(
                        name,
                        (handle) -> {
                            JSONArray rotation = (JSONArray) node.get("rotation");
                            if (rotation != null) {
                                Quaternionf quat =
                                        new Quaternionf(
                                                parseFloat(rotation.get(0)),
                                                parseFloat(rotation.get(1)),
                                                parseFloat(rotation.get(2)),
                                                parseFloat(rotation.get(3)));

                                handle.getTransform(Transform3D.class).rotate(quat);
                            }

                            JSONArray translation = (JSONArray) node.get("translation");
                            if (translation != null) {
                                Vector3f vec =
                                        new Vector3f(
                                                parseFloat(translation.get(0)),
                                                parseFloat(translation.get(1)),
                                                parseFloat(translation.get(2)));

                                handle.getTransform(Transform3D.class).setPosition(vec);
                            }

                            Integer mesh = parseInt(node, "mesh");
                            if (mesh != null) {
                                Renderable rend =
                                        new Renderable(
                                                mMeshes.get(mesh),
                                                mMaterials
                                                        .get(mMatIndices.get(mesh))
                                                        .incRefCount());
                                handle.addComponent(rend);
                            }

                            Integer camera = parseInt(node, "camera");
                            if (camera != null) handle.addComponent(mCameras.get(camera));

                            // TODO: parse lights

                            JSONArray children = (JSONArray) node.get("children");
                            if (children != null) {
                                for (Object cidx : children) {
                                    try {
                                        handle.addChild(parseNode(nodes, parseIntFromScalar(cidx)));
                                    } catch (ParseException e) {
                                        exception[0] = e;
                                        break;
                                    }
                                }
                            }
                        });

        if (exception[0] != null) throw exception[0];

        return ret;
    }

    public Scene getDefaultScene() {
        return mScenes.get(mDefaultScene);
    }

    public static Resource<GLTF> getResource(String name) {
        name = String.format("gltf/%s.gltf", name);

        return ResourceManager.getResource(
                GLTF.class,
                (buffer) -> {
                    try {
                        return new GLTF(new String(buffer));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                },
                name);
    }

    @Override
    public void free() {
        for (Scene scene : mScenes)
            for (GameObject go : scene.getGameObjects().toArray(GameObject[]::new))
                scene.destroyRootObjectImmediate(go);

        for (PBRMaterial mat : mMaterials) mat.free();
    }
}
