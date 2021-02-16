/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import java.util.ArrayList;
import org.dragonskulle.components.IOnAwake;
import org.junit.Assert;
import org.junit.Test;

public class GameObjectTest {

    // TODO: rewrite these tests to test every constructor

    /** Test whether a child GameObject's parent always has the child */
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
     * the root
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

        Assert.assertFalse(null instanceof IOnAwake);
    }

    /** Test whether a game object is removed from its parent when it is destroyed */
    @Test
    public void destroyRemovesFromParent() {
        GameObject root = new GameObject("root");
        GameObject child = new GameObject("child");

        root.addChild(child);

        child.destroy();

        // This should return an empty list since root only had one child
        ArrayList<GameObject> children = root.getChildren();

        String message = "Child wasn't removed from the parent when it was destroyed";
        Assert.assertEquals(message, 0, children.size());
    }

    /** Test whether every non-root GameObject has a root */
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

    /** Test whether a GameObject always has a transform */
    @Test
    public void transformNeverNull() {

        // Test both constructors for objects
        GameObject obj = new GameObject("obj");

        Assert.assertNotNull("GameObject did not have a transform", obj.getTransform());

        obj = new GameObject("obj", true);

        Assert.assertNotNull("GameObject did not have a transform", obj.getTransform());

        // Then check whether using the copy constructor still has a transform

        GameObject objClone = new GameObject(obj);

        Assert.assertNotNull(
                "GameObject did not have a transform after cloning", objClone.getTransform());
    }

    /*
    Tests still to do:

        Check that all components are returned and in the correct order





        If anyone has any suggestions feel free to add them


     */
}
