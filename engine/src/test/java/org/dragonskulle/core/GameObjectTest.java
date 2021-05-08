/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import org.dragonskulle.audio.components.AudioListener;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.Transform;
import org.dragonskulle.components.Transform3D;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.Assert;
import org.junit.Test;

public class GameObjectTest {

    private static class TestComponent extends Component {

        @Override
        protected void onDestroy() {}
    }

    /** Test whether a child GameObject's parent always has the child. */
    @Test
    public void parentContainsChild() {
        GameObject parent = new GameObject("parent");
        GameObject child = new GameObject("child");

        parent.addChild(child);

        boolean val = child.getParent().getChildren().contains(child);

        Assert.assertTrue("Child GameObject's parent did not contain child", val);
    }

    /**
     * Test whether a GameObject added as a child to a non-root object is still available through
     * the root.
     */
    @Test
    public void rootContainsChild() {
        GameObject root = new GameObject("root");
        GameObject child1 = new GameObject("1");
        GameObject child2 = new GameObject("2");

        root.addChild(child1);

        child1.addChild(child2);

        ArrayList<GameObject> children = new ArrayList<>();

        child2.getRoot().getAllChildren(children);

        boolean val = children.contains(child2);

        Assert.assertTrue("Child's root did not contain the child in all children", val);
    }

    /** Test whether a game object is removed from its parent when it is destroyed. */
    @Test
    public void destroyRemovesFromParent() {
        GameObject root = new GameObject("root");
        GameObject child = new GameObject("child");

        for (int i = 0; i < 5; i++) {
            child.addChild(child.createClone());
        }

        root.addChild(child);

        child.engineDestroy();

        // This should return an empty list since root only had one child
        ArrayList<GameObject> children = new ArrayList<>();
        root.getAllChildren(children);
        String message = "Child wasn't removed from the parent when it was destroyed";
        Assert.assertEquals(message, 0, children.size());
    }

    /** Test whether every non-root GameObject has a root. */
    @Test
    public void nonRootObjectNeverHasNullRoot() {
        GameObject root = new GameObject("root");

        // Add 5 children, each with 5 children, to the root
        for (int i = 0; i < 5; i++) {
            GameObject child = new GameObject(Integer.toString(i));
            for (int j = 0; j < 5; j++) {
                child.addChild(new GameObject(Integer.toString(j)));
            }
            root.addChild(child);
        }

        ArrayList<GameObject> children = new ArrayList<>();

        root.getAllChildren(children);

        for (GameObject child : children) {
            Assert.assertNotNull("Non-Root GameObject did not have a root", child.getRoot());
        }
    }

    /** Test whether a GameObject always has a transform. */
    @Test
    public void transformNeverNull() {

        // Test both constructors for objects
        GameObject obj = new GameObject("obj");

        Assert.assertNotNull("GameObject did not have a transform", obj.getTransform());

        obj = new GameObject("obj", true);

        Assert.assertNotNull("GameObject did not have a transform", obj.getTransform());

        // Then check whether using the copy constructor still has a transform

        GameObject objClone = obj.createClone();

        Assert.assertNotNull(
                "GameObject did not have a transform after cloning", objClone.getTransform());
    }

    /** Test whether updating a parent synchronizes its subchildren. */
    @Test
    public void indirectTransformsSynchronized() {
        GameObject subchild = new GameObject("subchild", new Transform3D(0f, 0f, 2f));

        GameObject root =
                new GameObject(
                        "parent",
                        new Transform3D(0f, 0f, -1f),
                        (go) -> {
                            // Rather than adding a child directly, build another child,
                            // and add it there
                            go.buildChild(
                                    "child",
                                    (child) -> {
                                        child.addChild(subchild);
                                    });
                        });

        // We initially start with world space Z coordinate being 2 - 1 = 1
        Assert.assertTrue(1f == subchild.getTransform().getPosition().z);

        root.getTransform(Transform3D.class).translate(0f, 0f, 1f);

        Assert.assertTrue(2f == subchild.getTransform().getPosition().z);

        root.getTransform(Transform3D.class).translate(0f, 0f, -1f);

        Assert.assertTrue(1f == subchild.getTransform().getPosition().z);
    }
    // The below tests were generated by https://www.diffblue.com/

    @Test
    public void testInstantiate() {
        GameObject actualInstantiateResult = GameObject.instantiate(new GameObject("Name"));
        assertTrue(actualInstantiateResult.getChildren().isEmpty());
        assertTrue(actualInstantiateResult.isRootObject());
        assertTrue(actualInstantiateResult.isEnabled());
        Transform transform = actualInstantiateResult.getTransform();
        assertTrue(transform instanceof Transform3D);
        assertNull(actualInstantiateResult.getRoot());
        assertTrue(actualInstantiateResult.getComponents().isEmpty());
        assertEquals("Name", actualInstantiateResult.getName());
        assertEquals(0, actualInstantiateResult.getDepth());
        assertTrue(transform.isEnabled());
        assertFalse(transform.isStarted());
        assertTrue(transform.getInvWorldMatrix() instanceof Matrix4f);
        assertSame(actualInstantiateResult, actualInstantiateResult.getReference().get());
        assertTrue(((Transform3D) transform).getLocalForward() instanceof Vector3f);
        assertEquals(transform.getMatrixForChildren(), ((Transform3D) transform).getLocalMatrix());
        assertFalse(transform.isAwake());
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).z, 0.0f);
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).y, 0.0f);
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).x, 0.0f);
        assertEquals(Float.NaN, ((Transform3D) transform).getLocalRotationAngles().y, 0.0f);
        assertEquals(1.0f, transform.getForwardVector().y, 0.0f);
        assertEquals(0.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).y, 0.0f);
        assertEquals(0.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).x, 0.0f);
        assertEquals(1.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).w, 0.0f);
        assertEquals(0.0f, ((Vector3f) ((Transform3D) transform).getLocalPosition()).y, 0.0f);
        assertEquals(0.0f, ((Vector3f) ((Transform3D) transform).getLocalPosition()).x, 0.0f);
        assertEquals(0.0f, transform.getRotation().y, 0.0f);
        assertEquals(0.0f, transform.getPosition().x, 0.0f);
        assertEquals(0.0f, ((Vector3f) ((Transform3D) transform).getLocalPosition()).z, 0.0f);
        assertEquals(0.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).z, 0.0f);
    }

    @Test
    public void testInstantiate2() {
        GameObject gameObject = new GameObject("Name");
        gameObject.addChild(new GameObject("Name"));
        GameObject actualInstantiateResult = GameObject.instantiate(gameObject);
        assertEquals(1, actualInstantiateResult.getChildren().size());
        assertTrue(actualInstantiateResult.isRootObject());
        assertTrue(actualInstantiateResult.isEnabled());
        Transform transform = actualInstantiateResult.getTransform();
        assertTrue(transform instanceof Transform3D);
        assertNull(actualInstantiateResult.getRoot());
        assertTrue(actualInstantiateResult.getComponents().isEmpty());
        assertEquals("Name", actualInstantiateResult.getName());
        assertEquals(0, actualInstantiateResult.getDepth());
        assertTrue(transform.isEnabled());
        assertFalse(transform.isStarted());
        assertTrue(transform.getInvWorldMatrix() instanceof Matrix4f);
        assertSame(actualInstantiateResult, actualInstantiateResult.getReference().get());
        assertTrue(((Transform3D) transform).getLocalForward() instanceof Vector3f);
        assertEquals(transform.getMatrixForChildren(), ((Transform3D) transform).getLocalMatrix());
        assertFalse(transform.isAwake());
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).z, 0.0f);
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).y, 0.0f);
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).x, 0.0f);
        assertEquals(Float.NaN, ((Transform3D) transform).getLocalRotationAngles().y, 0.0f);
        assertEquals(1.0f, transform.getForwardVector().y, 0.0f);
        assertEquals(0.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).y, 0.0f);
        assertEquals(0.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).x, 0.0f);
        assertEquals(1.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).w, 0.0f);
        assertEquals(0.0f, ((Vector3f) ((Transform3D) transform).getLocalPosition()).y, 0.0f);
        assertEquals(0.0f, ((Vector3f) ((Transform3D) transform).getLocalPosition()).x, 0.0f);
        assertEquals(0.0f, transform.getRotation().y, 0.0f);
        assertEquals(0.0f, transform.getPosition().x, 0.0f);
        assertEquals(0.0f, ((Vector3f) ((Transform3D) transform).getLocalPosition()).z, 0.0f);
        assertEquals(0.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).z, 0.0f);
    }

    @Test
    public void testInstantiate3() {
        GameObject gameObject = new GameObject("Name");
        gameObject.addComponent(new AudioListener());
        GameObject actualInstantiateResult = GameObject.instantiate(gameObject);
        assertTrue(actualInstantiateResult.getChildren().isEmpty());
        assertTrue(actualInstantiateResult.isRootObject());
        assertTrue(actualInstantiateResult.isEnabled());
        Transform transform = actualInstantiateResult.getTransform();
        assertTrue(transform instanceof Transform3D);
        assertNull(actualInstantiateResult.getRoot());
        assertEquals(1, actualInstantiateResult.getComponents().size());
        assertEquals("Name", actualInstantiateResult.getName());
        assertEquals(0, actualInstantiateResult.getDepth());
        assertFalse(transform.isStarted());
        assertTrue(transform.getInvWorldMatrix() instanceof Matrix4f);
        assertSame(actualInstantiateResult, actualInstantiateResult.getReference().get());
        assertTrue(((Transform3D) transform).getLocalForward() instanceof Vector3f);
        Matrix4fc worldMatrix = transform.getWorldMatrix();
        assertEquals(worldMatrix, ((Transform3D) transform).getLocalMatrix());
        assertFalse(transform.isAwake());
        assertTrue(transform.isEnabled());
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).z, 0.0f);
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).y, 0.0f);
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).x, 0.0f);
        assertEquals(Float.NaN, ((Transform3D) transform).getLocalRotationAngles().y, 0.0f);
        assertEquals(1.0f, transform.getForwardVector().y, 0.0f);
        assertEquals(0.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).y, 0.0f);
        assertEquals(0.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).x, 0.0f);
        assertEquals(1.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).w, 0.0f);
        assertEquals(0.0f, ((Vector3f) ((Transform3D) transform).getLocalPosition()).y, 0.0f);
        assertEquals(0.0f, ((Vector3f) ((Transform3D) transform).getLocalPosition()).x, 0.0f);
        assertEquals(0.0f, transform.getPosition().x, 0.0f);
        assertEquals(1.0f, worldMatrix.m00(), 0.0f);
        assertTrue(worldMatrix.isAffine());
        assertEquals(1.0f, worldMatrix.determinant3x3(), 0.0f);
        assertSame(worldMatrix, ((Matrix4f) worldMatrix).cofactor3x3());
        assertEquals(0.0f, ((Vector3f) ((Transform3D) transform).getLocalPosition()).z, 0.0f);
        assertEquals(0.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).z, 0.0f);
        assertEquals(1.0f, worldMatrix.m11(), 0.0f);
    }

    @Test
    public void testInstantiate4() {
        GameObject object = new GameObject("Name");
        Transform3D transform3D = new Transform3D();
        GameObject actualInstantiateResult = GameObject.instantiate(object, transform3D);
        assertTrue(actualInstantiateResult.getChildren().isEmpty());
        assertTrue(actualInstantiateResult.isRootObject());
        assertTrue(actualInstantiateResult.isEnabled());
        Transform transform = actualInstantiateResult.getTransform();
        assertSame(transform3D, transform);
        assertNull(actualInstantiateResult.getRoot());
        assertTrue(actualInstantiateResult.getComponents().isEmpty());
        assertEquals("Name", actualInstantiateResult.getName());
        assertEquals(0, actualInstantiateResult.getDepth());
        assertSame(actualInstantiateResult, transform.getGameObject());
        assertSame(actualInstantiateResult, actualInstantiateResult.getReference().get());
    }

    @Test
    public void testInstantiate5() {
        GameObject gameObject = new GameObject("Name");
        gameObject.addChild(new GameObject("Name"));
        Transform3D transform3D = new Transform3D();
        GameObject actualInstantiateResult = GameObject.instantiate(gameObject, transform3D);
        assertEquals(1, actualInstantiateResult.getChildren().size());
        assertTrue(actualInstantiateResult.isRootObject());
        assertTrue(actualInstantiateResult.isEnabled());
        Transform transform = actualInstantiateResult.getTransform();
        assertSame(transform3D, transform);
        assertNull(actualInstantiateResult.getRoot());
        assertTrue(actualInstantiateResult.getComponents().isEmpty());
        assertEquals("Name", actualInstantiateResult.getName());
        assertEquals(0, actualInstantiateResult.getDepth());
        assertSame(actualInstantiateResult, transform.getGameObject());
        assertSame(actualInstantiateResult, actualInstantiateResult.getReference().get());
    }

    @Test
    public void testInstantiate6() {
        GameObject gameObject = new GameObject("Name");
        gameObject.addComponent(new AudioListener());
        Transform3D transform3D = new Transform3D();
        GameObject actualInstantiateResult = GameObject.instantiate(gameObject, transform3D);
        assertTrue(actualInstantiateResult.getChildren().isEmpty());
        assertTrue(actualInstantiateResult.isRootObject());
        assertTrue(actualInstantiateResult.isEnabled());
        Transform transform = actualInstantiateResult.getTransform();
        assertSame(transform3D, transform);
        assertNull(actualInstantiateResult.getRoot());
        assertEquals(1, actualInstantiateResult.getComponents().size());
        assertEquals("Name", actualInstantiateResult.getName());
        assertEquals(0, actualInstantiateResult.getDepth());
        assertSame(actualInstantiateResult, transform.getGameObject());
        assertSame(actualInstantiateResult, actualInstantiateResult.getReference().get());
    }

    @Test
    public void testFindObjectByName() {
        assertNull(GameObject.findObjectByName("Name"));
    }

    @Test
    public void testConstructor() {
        GameObject actualGameObject = new GameObject("Name");
        assertTrue(actualGameObject.getChildren().isEmpty());
        assertTrue(actualGameObject.isRootObject());
        assertTrue(actualGameObject.isEnabled());
        Transform transform = actualGameObject.getTransform();
        assertTrue(transform instanceof Transform3D);
        assertNull(actualGameObject.getRoot());
        assertTrue(actualGameObject.getComponents().isEmpty());
        assertEquals("Name", actualGameObject.getName());
        assertEquals(0, actualGameObject.getDepth());
        assertTrue(transform.isEnabled());
        assertFalse(transform.isStarted());
        assertTrue(transform.getInvWorldMatrix() instanceof Matrix4f);
        assertSame(actualGameObject, actualGameObject.getReference().get());
        assertTrue(((Transform3D) transform).getLocalForward() instanceof Vector3f);
        assertEquals(transform.getMatrixForChildren(), ((Transform3D) transform).getLocalMatrix());
        assertFalse(transform.isAwake());
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).z, 0.0f);
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).y, 0.0f);
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).x, 0.0f);
        assertEquals(Float.NaN, ((Transform3D) transform).getLocalRotationAngles().y, 0.0f);
        assertEquals(1.0f, transform.getForwardVector().y, 0.0f);
        assertEquals(1.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).w, 0.0f);
        assertEquals(0.0f, transform.getRotation().y, 0.0f);
        assertEquals(0.0f, transform.getPosition().x, 0.0f);
    }

    @Test
    public void testConstructor2() {
        Transform3D transform3D = new Transform3D();
        GameObject actualGameObject = new GameObject("Name", transform3D);
        assertTrue(actualGameObject.getChildren().isEmpty());
        assertTrue(actualGameObject.isRootObject());
        assertTrue(actualGameObject.isEnabled());
        Transform transform = actualGameObject.getTransform();
        assertSame(transform3D, transform);
        assertNull(actualGameObject.getRoot());
        assertTrue(actualGameObject.getComponents().isEmpty());
        assertEquals("Name", actualGameObject.getName());
        assertEquals(0, actualGameObject.getDepth());
        assertSame(actualGameObject, transform.getGameObject());
        assertSame(actualGameObject, actualGameObject.getReference().get());
    }

    @Test
    public void testConstructor3() {
        Transform3D transform3D = new Transform3D(10.0f, 10.0f, 10.0f);
        GameObject actualGameObject =
                new GameObject("org.dragonskulle.components.Transform", transform3D);
        assertTrue(actualGameObject.getChildren().isEmpty());
        assertTrue(actualGameObject.isRootObject());
        assertTrue(actualGameObject.isEnabled());
        Transform transform = actualGameObject.getTransform();
        assertSame(transform3D, transform);
        assertNull(actualGameObject.getRoot());
        assertTrue(actualGameObject.getComponents().isEmpty());
        assertEquals("org.dragonskulle.components.Transform", actualGameObject.getName());
        assertEquals(0, actualGameObject.getDepth());
        assertSame(actualGameObject, transform.getGameObject());
        assertSame(actualGameObject, actualGameObject.getReference().get());
    }

    @Test
    public void testConstructor4() {
        GameObject actualGameObject = new GameObject("Name", true);
        assertTrue(actualGameObject.getChildren().isEmpty());
        assertTrue(actualGameObject.isRootObject());
        assertTrue(actualGameObject.isEnabled());
        Transform transform = actualGameObject.getTransform();
        assertTrue(transform instanceof Transform3D);
        assertNull(actualGameObject.getRoot());
        assertTrue(actualGameObject.getComponents().isEmpty());
        assertEquals("Name", actualGameObject.getName());
        assertEquals(0, actualGameObject.getDepth());
        assertTrue(transform.isEnabled());
        assertFalse(transform.isStarted());
        assertTrue(transform.getInvWorldMatrix() instanceof Matrix4f);
        assertSame(actualGameObject, actualGameObject.getReference().get());
        assertTrue(((Transform3D) transform).getLocalForward() instanceof Vector3f);
        assertEquals(transform.getMatrixForChildren(), ((Transform3D) transform).getLocalMatrix());
        assertFalse(transform.isAwake());
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).z, 0.0f);
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).y, 0.0f);
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).x, 0.0f);
        assertEquals(Float.NaN, ((Transform3D) transform).getLocalRotationAngles().y, 0.0f);
        assertEquals(1.0f, transform.getForwardVector().y, 0.0f);
        assertEquals(1.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).w, 0.0f);
        assertEquals(0.0f, transform.getRotation().y, 0.0f);
        assertEquals(0.0f, transform.getPosition().x, 0.0f);
    }

    @Test
    public void testConstructor5() {
        Transform3D transform3D = new Transform3D();
        GameObject actualGameObject = new GameObject("Name", true, transform3D);
        assertTrue(actualGameObject.getChildren().isEmpty());
        assertTrue(actualGameObject.isRootObject());
        assertTrue(actualGameObject.isEnabled());
        Transform transform = actualGameObject.getTransform();
        assertSame(transform3D, transform);
        assertNull(actualGameObject.getRoot());
        assertTrue(actualGameObject.getComponents().isEmpty());
        assertEquals("Name", actualGameObject.getName());
        assertEquals(0, actualGameObject.getDepth());
        assertSame(actualGameObject, transform.getGameObject());
        assertSame(actualGameObject, actualGameObject.getReference().get());
    }

    @Test
    public void testConstructor6() {
        Transform3D transform3D = new Transform3D();
        GameObject actualGameObject =
                new GameObject("org.dragonskulle.components.Transform", false, transform3D);
        assertTrue(actualGameObject.getChildren().isEmpty());
        assertTrue(actualGameObject.isRootObject());
        assertFalse(actualGameObject.isEnabled());
        Transform transform = actualGameObject.getTransform();
        assertSame(transform3D, transform);
        assertNull(actualGameObject.getRoot());
        assertTrue(actualGameObject.getComponents().isEmpty());
        assertEquals("org.dragonskulle.components.Transform", actualGameObject.getName());
        assertEquals(0, actualGameObject.getDepth());
        assertSame(actualGameObject, transform.getGameObject());
        assertSame(actualGameObject, actualGameObject.getReference().get());
    }

    @Test
    public void testGetComponent() {
        GameObject gameObject = new GameObject("Name");
        assertNull(gameObject.<Component>getComponent(Component.class));
    }

    @Test
    public void testGetComponent2() {
        GameObject gameObject = new GameObject("Name");
        gameObject.addComponent(new AudioListener());
        assertNull(gameObject.getComponent(Transform.class));
    }

    @Test
    public void testGetComponentsInChildren() {
        GameObject gameObject = new GameObject("Name");
        gameObject.getComponentsInChildren(Component.class, new ArrayList<>());
        assertTrue(gameObject.isRootObject());
        assertTrue(gameObject.isEnabled());
        assertTrue(gameObject.getTransform() instanceof org.dragonskulle.components.Transform3D);
        assertEquals(0, gameObject.getDepth());
    }

    @Test
    public void testGetAllChildren() {
        GameObject gameObject = new GameObject("Name");
        gameObject.getAllChildren(new ArrayList<GameObject>());
        assertTrue(gameObject.isRootObject());
        assertTrue(gameObject.isEnabled());
        assertTrue(gameObject.getTransform() instanceof org.dragonskulle.components.Transform3D);
        assertEquals(0, gameObject.getDepth());
    }

    @Test
    public void testGetAllChildren2() {
        GameObject gameObject = new GameObject("Name");
        gameObject.addChild(new GameObject("Name"));
        ArrayList<GameObject> gameObjectList = new ArrayList<GameObject>();
        gameObject.getAllChildren(gameObjectList);
        assertEquals(1, gameObjectList.size());
    }

    @Test
    public void testFindChildByName() {
        assertNull((new GameObject("Name")).findChildByName("Name"));
    }

    @Test
    public void testFindChildByName2() {
        GameObject gameObject = new GameObject("Name");
        GameObject gameObject1 = new GameObject("Name");
        gameObject.addChild(gameObject1);
        assertSame(gameObject1, gameObject.findChildByName("Name"));
    }

    @Test
    public void testFindChildByName3() {
        GameObject gameObject = new GameObject("Name");
        gameObject.addChild(new GameObject("org.dragonskulle.components.Transform"));
        assertNull(gameObject.findChildByName("Name"));
    }

    @Test
    public void testAddComponent() {
        GameObject gameObject = new GameObject("Name");
        AudioListener audioListener = new AudioListener();
        gameObject.addComponent(audioListener);
        GameObject gameObject1 = audioListener.getGameObject();
        assertSame(gameObject, gameObject1);
        assertEquals(1, gameObject1.getComponents().size());
    }

    @Test
    public void testAddChild() {
        GameObject gameObject = new GameObject("Name");
        GameObject gameObject1 = new GameObject("Name");
        gameObject.addChild(gameObject1);
        assertFalse(gameObject1.isRootObject());
        assertTrue(gameObject1.isEnabled());
        assertSame(gameObject, gameObject1.getRoot());
        assertEquals(1, gameObject1.getDepth());
    }

    @Test
    public void testAddChild2() {
        GameObject gameObject = new GameObject("Name");

        GameObject gameObject1 = new GameObject("Name");
        gameObject1.addChild(new GameObject("Name"));
        gameObject.addChild(gameObject1);
        assertFalse(gameObject1.isRootObject());
        assertTrue(gameObject1.isEnabled());
        assertSame(gameObject, gameObject1.getRoot());
        assertEquals(1, gameObject1.getDepth());
    }

    @Test
    public void testRemoveComponent() {
        GameObject gameObject = new GameObject("Name");
        gameObject.removeComponent(new AudioListener());
        assertTrue(gameObject.getComponents().isEmpty());
    }

    @Test
    public void testRecreateReferences() {
        GameObject gameObject = new GameObject("Name");
        gameObject.recreateReferences();
        assertSame(gameObject, gameObject.getReference().get());
    }

    @Test
    public void testRecreateReferences2() {
        GameObject gameObject = new GameObject("Name");
        gameObject.addChild(new GameObject("Name"));
        gameObject.recreateReferences();
        assertSame(gameObject, gameObject.getReference().get());
    }

    @Test
    public void testRecreateReferences3() {
        GameObject gameObject = new GameObject("Name");
        gameObject.addComponent(new AudioListener());
        gameObject.recreateReferences();
        assertSame(gameObject, gameObject.getReference().get());
    }

    @Test
    public void testCreateClone() {
        GameObject actualCreateCloneResult = (new GameObject("Name")).createClone();
        assertTrue(actualCreateCloneResult.getChildren().isEmpty());
        assertTrue(actualCreateCloneResult.isRootObject());
        assertTrue(actualCreateCloneResult.isEnabled());
        Transform transform = actualCreateCloneResult.getTransform();
        assertTrue(transform instanceof Transform3D);
        assertNull(actualCreateCloneResult.getRoot());
        assertTrue(actualCreateCloneResult.getComponents().isEmpty());
        assertEquals("Name", actualCreateCloneResult.getName());
        assertEquals(0, actualCreateCloneResult.getDepth());
        assertTrue(transform.isEnabled());
        assertFalse(transform.isStarted());
        assertTrue(transform.getInvWorldMatrix() instanceof Matrix4f);
        assertSame(actualCreateCloneResult, actualCreateCloneResult.getReference().get());
        assertTrue(((Transform3D) transform).getLocalForward() instanceof Vector3f);
        assertEquals(transform.getMatrixForChildren(), ((Transform3D) transform).getLocalMatrix());
        assertFalse(transform.isAwake());
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).z, 0.0f);
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).y, 0.0f);
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).x, 0.0f);
        assertEquals(Float.NaN, ((Transform3D) transform).getLocalRotationAngles().y, 0.0f);
        assertEquals(1.0f, transform.getForwardVector().y, 0.0f);
        assertEquals(0.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).y, 0.0f);
        assertEquals(0.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).x, 0.0f);
        assertEquals(1.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).w, 0.0f);
        assertEquals(0.0f, ((Vector3f) ((Transform3D) transform).getLocalPosition()).y, 0.0f);
        assertEquals(0.0f, ((Vector3f) ((Transform3D) transform).getLocalPosition()).x, 0.0f);
        assertEquals(0.0f, transform.getRotation().y, 0.0f);
        assertEquals(0.0f, transform.getPosition().x, 0.0f);
        assertEquals(0.0f, ((Vector3f) ((Transform3D) transform).getLocalPosition()).z, 0.0f);
        assertEquals(0.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).z, 0.0f);
    }

    @Test
    public void testCreateClone2() {
        GameObject gameObject = new GameObject("Name");
        gameObject.addChild(new GameObject("Name"));
        GameObject actualCreateCloneResult = gameObject.createClone();
        assertEquals(1, actualCreateCloneResult.getChildren().size());
        assertTrue(actualCreateCloneResult.isRootObject());
        assertTrue(actualCreateCloneResult.isEnabled());
        Transform transform = actualCreateCloneResult.getTransform();
        assertTrue(transform instanceof Transform3D);
        assertNull(actualCreateCloneResult.getRoot());
        assertTrue(actualCreateCloneResult.getComponents().isEmpty());
        assertEquals("Name", actualCreateCloneResult.getName());
        assertEquals(0, actualCreateCloneResult.getDepth());
        assertTrue(transform.isEnabled());
        assertFalse(transform.isStarted());
        assertTrue(transform.getInvWorldMatrix() instanceof Matrix4f);
        assertSame(actualCreateCloneResult, actualCreateCloneResult.getReference().get());
        assertTrue(((Transform3D) transform).getLocalForward() instanceof Vector3f);
        assertEquals(transform.getMatrixForChildren(), ((Transform3D) transform).getLocalMatrix());
        assertFalse(transform.isAwake());
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).z, 0.0f);
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).y, 0.0f);
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).x, 0.0f);
        assertEquals(Float.NaN, ((Transform3D) transform).getLocalRotationAngles().y, 0.0f);
        assertEquals(1.0f, transform.getForwardVector().y, 0.0f);
        assertEquals(0.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).y, 0.0f);
        assertEquals(0.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).x, 0.0f);
        assertEquals(1.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).w, 0.0f);
        assertEquals(0.0f, ((Vector3f) ((Transform3D) transform).getLocalPosition()).y, 0.0f);
        assertEquals(0.0f, ((Vector3f) ((Transform3D) transform).getLocalPosition()).x, 0.0f);
        assertEquals(0.0f, transform.getRotation().y, 0.0f);
        assertEquals(0.0f, transform.getPosition().x, 0.0f);
        assertEquals(0.0f, ((Vector3f) ((Transform3D) transform).getLocalPosition()).z, 0.0f);
        assertEquals(0.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).z, 0.0f);
    }

    @Test
    public void testCreateClone3() {
        GameObject gameObject = new GameObject("Name");
        gameObject.addComponent(new AudioListener());
        GameObject actualCreateCloneResult = gameObject.createClone();
        assertTrue(actualCreateCloneResult.getChildren().isEmpty());
        assertTrue(actualCreateCloneResult.isRootObject());
        assertTrue(actualCreateCloneResult.isEnabled());
        Transform transform = actualCreateCloneResult.getTransform();
        assertTrue(transform instanceof Transform3D);
        assertNull(actualCreateCloneResult.getRoot());
        assertEquals(1, actualCreateCloneResult.getComponents().size());
        assertEquals("Name", actualCreateCloneResult.getName());
        assertEquals(0, actualCreateCloneResult.getDepth());
        assertFalse(transform.isStarted());
        assertTrue(transform.getInvWorldMatrix() instanceof Matrix4f);
        assertSame(actualCreateCloneResult, actualCreateCloneResult.getReference().get());
        assertTrue(((Transform3D) transform).getLocalForward() instanceof Vector3f);
        Matrix4fc worldMatrix = transform.getWorldMatrix();
        assertEquals(worldMatrix, ((Transform3D) transform).getLocalMatrix());
        assertFalse(transform.isAwake());
        assertTrue(transform.isEnabled());
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).z, 0.0f);
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).y, 0.0f);
        assertEquals(1.0f, ((Vector3f) ((Transform3D) transform).getLocalScale()).x, 0.0f);
        assertEquals(Float.NaN, ((Transform3D) transform).getLocalRotationAngles().y, 0.0f);
        assertEquals(1.0f, transform.getForwardVector().y, 0.0f);
        assertEquals(0.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).y, 0.0f);
        assertEquals(0.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).x, 0.0f);
        assertEquals(1.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).w, 0.0f);
        assertEquals(0.0f, ((Vector3f) ((Transform3D) transform).getLocalPosition()).y, 0.0f);
        assertEquals(0.0f, ((Vector3f) ((Transform3D) transform).getLocalPosition()).x, 0.0f);
        assertEquals(0.0f, transform.getPosition().x, 0.0f);
        assertEquals(1.0f, worldMatrix.m00(), 0.0f);
        assertTrue(worldMatrix.isAffine());
        assertEquals(1.0f, worldMatrix.determinant3x3(), 0.0f);
        assertSame(worldMatrix, ((Matrix4f) worldMatrix).cofactor3x3());
        assertEquals(0.0f, ((Vector3f) ((Transform3D) transform).getLocalPosition()).z, 0.0f);
        assertEquals(0.0f, ((Quaternionf) ((Transform3D) transform).getLocalRotation()).z, 0.0f);
        assertEquals(1.0f, worldMatrix.m11(), 0.0f);
    }

    @Test
    public void testGetChildren() {
        assertTrue((new GameObject("Name")).getChildren().isEmpty());
    }

    @Test
    public void testGetTransform() {
        GameObject gameObject = new GameObject("Name");
        assertTrue(
                gameObject.<Transform>getTransform(Transform.class)
                        instanceof org.dragonskulle.components.Transform3D);
    }

    @Test
    public void testGetParentTransform() {
        assertNull((new GameObject("Name")).getParentTransform());
    }

    @Test
    public void testIsRootObject() {
        assertTrue((new GameObject("Name")).isRootObject());
    }

    @Test
    public void testSetEnabled() {
        GameObject gameObject = new GameObject("Name");
        gameObject.setEnabled(true);
        assertTrue(gameObject.isEnabled());
    }

    @Test
    public void testSetEnabled2() {
        GameObject gameObject = new GameObject("Name");
        gameObject.addChild(new GameObject("Name"));
        gameObject.setEnabled(true);
        assertTrue(gameObject.isEnabled());
    }

    @Test
    public void testSetDepth() {
        GameObject gameObject = new GameObject("Name");
        gameObject.setDepth(2);
        assertEquals(2, gameObject.getDepth());
    }

    @Test
    public void testSetDepth2() {
        GameObject gameObject = new GameObject("Name");
        gameObject.addChild(new GameObject("Name"));
        gameObject.setDepth(2);
        assertEquals(2, gameObject.getDepth());
    }

    @Test
    public void testEngineDestroy() {
        GameObject gameObject = new GameObject("Name");
        gameObject.engineDestroy();
        assertNull(gameObject.getTransform());
        assertNull(gameObject.getRoot());
        assertNull(gameObject.getReference().get());
    }

    @Test
    public void testEngineDestroy2() {
        GameObject gameObject = new GameObject("Name");
        gameObject.addChild(new GameObject("Name"));
        gameObject.engineDestroy();
        assertNull(gameObject.getTransform());
        assertNull(gameObject.getRoot());
        assertNull(gameObject.getReference().get());
    }

    /*
    Tests still to do:
    /** Test that an instantiated GameObject is setup correctly */
    @Test
    public void instantiatePerformedCorrectly() {
        String parentName = "parent";
        String childName = "child";

        // Setup the object
        GameObject object = new GameObject(parentName);
        GameObject objectChild = new GameObject(childName);
        TestComponent component = new TestComponent();

        objectChild.addComponent(component);
        object.addChild(objectChild);

        // Create the clone
        GameObject clonedObject = object.createClone();

        // Check that the clonedObject is not the same as the original
        Assert.assertNotEquals(
                "Cloned object was the same object as the original", object, clonedObject);

        // Check that the number of children are equal
        Assert.assertEquals(
                "Number of children on cloned object incorrect",
                1,
                clonedObject.getChildren().size());

        GameObject clonedObjectChild = clonedObject.getChildren().get(0);

        // Check that the child is not the same as the original
        Assert.assertNotEquals(
                "Child was the same object as the original", objectChild, clonedObjectChild);

        // Check the name's of the GameObject's
        Assert.assertEquals(
                "Cloned object's name was incorrect", parentName, clonedObject.getName());
        Assert.assertEquals(
                "Cloned object's child's name was incorrect",
                childName,
                clonedObjectChild.getName());

        // Check that the child's parent is correct
        Assert.assertEquals(
                "Cloned object's child had incorrect parent",
                clonedObject,
                clonedObjectChild.getParent());

        // Check that the component exists in the child
        Assert.assertEquals(
                "Child did not contain the component", 1, clonedObjectChild.getComponents().size());

        // Check that the type of the component is correct
        Component clonedComponent = clonedObjectChild.getComponents().get(0);

        Assert.assertTrue(
                "Component had the incorrect type", clonedComponent instanceof TestComponent);
    }
}
