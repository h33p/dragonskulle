package org.dragonskulle.core;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnAwake;
import org.junit.Test;
import org.junit.Assert;

import java.util.ArrayList;

public class GameObjectTest {

    static class TestComponent extends Component implements IOnAwake {

        @Override
        public void onAwake() {
            System.out.println("YeeHaw");
        }
    }

    /**
     * Test whether a child GameObject's parent always has the child
     */
    @Test
    public void parentContainsChild() {
        GameObject parent = new GameObject("parent");
        GameObject child = new GameObject("child");

        parent.addChild(child);

        boolean val = child.getParent().getChildren().contains(child);

        Assert.assertTrue("Child GameObject's parent did not contain child\n", val);
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


        boolean val = child2.getRoot().getAllChildren().contains(child2.getReference());

        Assert.assertTrue("Child's root did not contain the child in all children", val);

        Assert.assertFalse(null instanceof IOnAwake);
    }

    /**
     * Test whether a game object is removed from its parent when it is destroyed
     */
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

    /**
     * Test whether every non-root GameObject has a root
     */
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

        for (Reference<GameObject> childRef : root.getAllChildren()) {
            // This won't be null since we just created the objects
            GameObject child = childRef.get();

            Assert.assertNotNull("Non-Root GameObject did not have a root", child.getRoot());
        }

    }

    /**
     * Test whether every GameObject has a transform or not
     */
    @Test
    public void transformNeverNull() {
        GameObject obj = new GameObject("obj");

        // TODO: Uncomment this when Transforms have been implemented
        //Assert.assertNotNull(obj.getTransform());
    }

    /*
    Tests still to do:

        Check that all components are returned and in the correct order





        If anyone has any suggestions feel free to add them


     */
}
