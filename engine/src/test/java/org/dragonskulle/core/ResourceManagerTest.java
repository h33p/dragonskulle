/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import static org.junit.Assert.*;

import org.junit.Test;

/** Unit tests for Resource Manager. */
public class ResourceManagerTest {

    private static boolean throwOnLoad = false;

    static {
        ResourceManager.registerResource(
                TestBytes.class, (a) -> "text/" + a.getName(), (b, __) -> new TestBytes(b));
        ResourceManager.registerResource(
                TestLines.class, (a) -> "text/" + a.getName(), (b, __) -> new TestLines(b));
    }

    /** First class for simple text resource loading */
    private static class TestBytes {
        private byte[] buf;

        private TestBytes(byte[] buf) {
            if (throwOnLoad) throw new RuntimeException("Was asked to throw!");
            this.buf = buf;
        }

        public static Resource<TestBytes> getResource(String name) {
            return ResourceManager.getResource(TestBytes.class, name);
        }

        public static void unlinkResource(String name) {
            ResourceManager.unlinkResource(TestBytes.class, name);
        }
    }

    /** Second class for simple text resource loading */
    private static class TestLines implements AutoCloseable {
        private String[] lines;
        private boolean wasClosed = false;

        private TestLines(byte[] buf) {
            if (throwOnLoad) throw new RuntimeException("Was asked to throw!");
            lines = (new String(buf)).split("\\r?\\n");
        }

        public static Resource<TestLines> getResource(String name) {
            return ResourceManager.getResource(TestLines.class, name);
        }

        @Override
        public void close() {
            assertFalse(wasClosed);
            wasClosed = true;
        }
    }

    /** Simple test for seeing if loading works */
    @Test
    public void simpleLoad() {
        try (Resource<TestBytes> res = TestBytes.getResource("a.txt")) {
            assertNotNull(res != null);
            assertNotNull(res.get() != null);
        }

        try (Resource<TestLines> res = TestLines.getResource("b.txt")) {
            assertNotNull(res != null);
            assertNotNull(res.get() != null);
            TestLines lines = res.get();
            assertEquals(1, lines.lines.length);
        }
    }

    /**
     * Test property of cached loading
     *
     * <p>It should be possible to load a resource, and get() call should point to the same object.
     */
    @Test
    public void cachedLoad() {
        try (Resource<TestBytes> res = TestBytes.getResource("a.txt")) {
            assertNotNull(res);
            assertNotNull(res.get());

            try (Resource<TestBytes> res2 = TestBytes.getResource("a.txt")) {
                assertNotNull(res2);
                assertNotNull(res2.get());
                assertSame(res.get(), res2.get());
            }
        }

        try (Resource<TestBytes> res = TestBytes.getResource("a.txt")) {
            assertNotNull(res);
            assertNotNull(res.get());

            try (Resource<TestBytes> res2 = TestBytes.getResource("a.txt")) {
                assertNotNull(res2);
                assertNotNull(res2.get());
                assertSame(res.get(), res2.get());
            }
        }

        try (Resource<TestLines> res = TestLines.getResource("a.txt")) {
            assertNotNull(res);
            assertNotNull(res.get());

            try (Resource<TestLines> res2 = TestLines.getResource("a.txt")) {
                assertNotNull(res2);
                assertNotNull(res2.get());
                assertSame(res.get(), res2.get());
            }
        }
    }

    /**
     * Test whether unlinking functions as expected
     *
     * <p>Loading second resource after unlinking should yield different reference, and loading a
     * third instance of the resource should yield the same reference as the second. Loading the
     * fourth reference after the first one is freed should yield the same reference as the 2nd, and
     * the 3rd. After freeing all these references the new reference should be unique.
     */
    @Test
    public void unlinking() {
        Resource<TestBytes> res1 = TestBytes.getResource("a.txt");

        assertNotNull(res1);

        TestBytes.unlinkResource("a.txt");

        Resource<TestBytes> res2 = TestBytes.getResource("a.txt");
        assertNotNull(res2);
        assertNotSame(res1.get(), res2.get());

        Resource<TestBytes> res3 = TestBytes.getResource("a.txt");
        assertNotNull(res3);
        assertSame(res2.get(), res3.get());

        res1.free();

        Resource<TestBytes> res4 = TestBytes.getResource("a.txt");
        assertNotNull(res4);
        assertSame(res3.get(), res4.get());

        TestBytes cached = res2.get();
        res2.free();
        res3.free();
        res4.free();

        Resource<TestBytes> res5 = TestBytes.getResource("a.txt");
        assertNotNull(res4);
        assertNotSame(cached, res5.get());

        res5.free();
    }

    /**
     * Test whether reloading functions as expected
     *
     * <p>Upon reload, all {@code Resource} instances should point to a new instance of {@code
     * TestBytes}.
     */
    @Test
    public void reloading() {
        try (Resource<TestBytes> res = TestBytes.getResource("a.txt")) {
            assertNotNull(res);

            try (Resource<TestBytes> res2 = TestBytes.getResource("a.txt")) {
                assertNotNull(res2);
                assertSame(res.get(), res2.get());
                TestBytes cached = res.get();
                assertTrue(res.reload());
                assertNotSame(cached, res.get());
                assertSame(res.get(), res2.get());
                assertEquals(cached.buf.length, res.get().buf.length);
            }
        }
    }

    /**
     * Test whether we can load a resource after it initially fails to load
     *
     * <p>This will check whether we can load a resource when it fails to load, or whether the
     * resource manager gets stuck in a bugged state.
     */
    @Test
    public void reloadAfterFail() {
        throwOnLoad = true;
        try (Resource<TestBytes> res = TestBytes.getResource("a.txt")) {
            assertNull(res);
        }
        throwOnLoad = false;
        try (Resource<TestBytes> res = TestBytes.getResource("a.txt")) {
            assertNotNull(res);
        }
    }

    /**
     * Test whether resource gets automatically closed
     *
     * <p>This will use try-with-resources syntax to automatically close the resource. It is
     * strongly discouraged against taking the reference out of Resource for this very reason - the
     * instance can be closed at any point when the reference count drops to 0.
     */
    @Test
    public void autoClose() {
        TestLines lines = null;
        try (Resource<TestLines> res = TestLines.getResource("a.txt")) {
            assertNotNull(res);
            lines = res.get();
        }
        assertNotNull(lines);
        assertTrue(lines.wasClosed);
    }
}
