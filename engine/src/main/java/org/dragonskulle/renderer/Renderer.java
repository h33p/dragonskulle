/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static java.util.stream.Collectors.toSet;
import static org.dragonskulle.utils.Env.*;
import static org.dragonskulle.utils.Env.envString;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.dragonskulle.components.Camera;
import org.dragonskulle.components.Renderable;
import org.dragonskulle.core.Resource;
import org.dragonskulle.renderer.TextureMapping.TextureFiltering;
import org.dragonskulle.renderer.TextureMapping.TextureWrapping;
import org.joml.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.*;

/**
 * Vulkan renderer
 *
 * @author Aurimas Blažulionis
 *     <p>This renderer allows to draw {@code Renderable} objects on screen. Application needs to
 *     call {@code onResized} when its window gets resized.
 *     <p>This renderer was originally based on<a href="https://vulkan-tutorial.com/">Vulkan
 *     Tutorial</a>, and was later rewritten with a much more manageable design.
 */
public class Renderer implements NativeResource {

    private long mWindow;

    private VkInstance mInstance;
    private long mDebugMessenger;
    private long mSurface;

    private PhysicalDevice mPhysicalDevice;

    private VkDevice mDevice;
    private VkQueue mGraphicsQueue;
    private VkQueue mPresentQueue;

    private long mCommandPool;
    private long mDescriptorSetLayout;

    private TextureSamplerFactory mSamplerFactory;
    private TmpObjectState mObjectState;

    private VkSurfaceFormatKHR mSurfaceFormat;
    private VkExtent2D mExtent;
    private long mSwapchain;
    private long mDescriptorPool;
    private long mRenderPass;

    private ImageContext[] mImageContexts;
    private FrameContext[] mFrameContexts;

    private int mFrameCounter = 0;

    private static final List<String> WANTED_VALIDATION_LAYERS_LIST =
            Arrays.asList("VK_LAYER_KHRONOS_validation");
    private static final Set<String> DEVICE_EXTENSIONS =
            Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME).collect(toSet());

    public static final Logger LOGGER = Logger.getLogger("render");
    public static final boolean DEBUG_MODE = envBool("DEBUG_RENDERER", false);
    private static final String TARGET_GPU = envString("TARGET_GPU", null);

    private static final long UINT64_MAX = -1L;
    private static final int FRAMES_IN_FLIGHT = 4;

    /** Synchronization objects for when multiple frames are rendered at a time */
    private class FrameContext {
        public long imageAvailableSemaphore;
        public long renderFinishedSemaphore;
        public long inFlightFence;
    }

    /** All state for a single frame */
    private static class ImageContext {
        public long descriptorSet;
        public VkCommandBuffer commandBuffer;
        public long inFlightFence;
        public long framebuffer;
        // TODO: Remove this
        public UniformBufferObject ubo;

        private long mImageView;
        private VkDevice mDevice;

        // TODO: Remove this
        private VulkanBuffer mBuffer;
        private VulkanImage mTextureImage;
        private long mTextureImageView;

        private static final TextureMapping MAPPING =
                new TextureMapping(TextureFiltering.LINEAR, TextureWrapping.REPEAT);

        /** Create a image context */
        private ImageContext(
                Renderer renderer, long image, long descriptorSet, long commandBuffer) {
            this.descriptorSet = descriptorSet;
            this.commandBuffer = new VkCommandBuffer(commandBuffer, renderer.mDevice);
            this.mImageView = renderer.createImageView(image);
            this.framebuffer = createFramebuffer(renderer);

            this.mDevice = renderer.mDevice;

            ubo = new UniformBufferObject(new Matrix4f(), new Matrix4f(), new Matrix4f());
            ubo.view.lookAt(
                    new Vector3f(2.0f, 2.0f, 2.0f),
                    new Vector3f(0.0f, 0.0f, -0.05f),
                    new Vector3f(0.0f, 0.0f, 1.0f));
            ubo.proj.setPerspective(
                    45.0f,
                    (float) renderer.mExtent.width() / (float) renderer.mExtent.height(),
                    0.1f,
                    10.f,
                    true);
            ubo.proj.m11(-ubo.proj.m11());

            long size = UniformBufferObject.SIZEOF;
            mBuffer =
                    new VulkanBuffer(
                            renderer.mDevice,
                            renderer.mPhysicalDevice,
                            size,
                            VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                    | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

            try (Resource<Texture> resource = Texture.getResource("test_cc0_texture.jpg")) {
                if (resource == null) throw new RuntimeException("Failed to load texture!");
                Texture texture = resource.get();
                VkCommandBuffer tmpCommandBuffer = renderer.beginSingleUseCommandBuffer();
                mTextureImage =
                        new VulkanImage(
                                texture,
                                tmpCommandBuffer,
                                renderer.mDevice,
                                renderer.mPhysicalDevice);
                renderer.endSingleUseCommandBuffer(tmpCommandBuffer);
                mTextureImage.freeStagingBuffer();
            }
            mTextureImageView =
                    renderer.createImageView(mTextureImage.image, VK_FORMAT_R8G8B8A8_SRGB);
        }

        /**
         * Update descriptor sets
         *
         * <p>This method will simply update the attached descriptor set to have correct layout for
         * textures and uniform buffer.
         */
        private void updateDescriptorSet(Renderer renderer) {
            try (MemoryStack stack = stackPush()) {
                VkDescriptorBufferInfo.Buffer bufferInfo =
                        VkDescriptorBufferInfo.callocStack(1, stack);
                bufferInfo.offset(0);
                bufferInfo.range(UniformBufferObject.SIZEOF);
                bufferInfo.buffer(mBuffer.buffer);

                VkDescriptorImageInfo.Buffer imageInfo =
                        VkDescriptorImageInfo.callocStack(1, stack);
                imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                imageInfo.imageView(mTextureImageView);
                imageInfo.sampler(renderer.mSamplerFactory.getSampler(MAPPING));

                VkWriteDescriptorSet.Buffer descriptorWrites =
                        VkWriteDescriptorSet.callocStack(2, stack);

                VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get(0);
                uboDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                uboDescriptorWrite.dstBinding(0);
                uboDescriptorWrite.dstArrayElement(0);
                uboDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uboDescriptorWrite.descriptorCount(1);
                uboDescriptorWrite.pBufferInfo(bufferInfo);
                uboDescriptorWrite.dstSet(descriptorSet);

                VkWriteDescriptorSet samplerDescriptorWrite = descriptorWrites.get(1);
                samplerDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                samplerDescriptorWrite.dstBinding(1);
                samplerDescriptorWrite.dstArrayElement(0);
                samplerDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                samplerDescriptorWrite.descriptorCount(1);
                samplerDescriptorWrite.pImageInfo(imageInfo);
                samplerDescriptorWrite.dstSet(descriptorSet);

                vkUpdateDescriptorSets(renderer.mDevice, descriptorWrites, null);
            }
        }

        /** Create a framebuffer from image view */
        private long createFramebuffer(Renderer renderer) {
            try (MemoryStack stack = stackPush()) {
                VkFramebufferCreateInfo createInfo = VkFramebufferCreateInfo.callocStack(stack);
                createInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);

                createInfo.renderPass(renderer.mRenderPass);
                createInfo.width(renderer.mExtent.width());
                createInfo.height(renderer.mExtent.height());
                createInfo.layers(1);

                LongBuffer attachment = stack.longs(mImageView);
                LongBuffer framebuffer = stack.longs(0);

                createInfo.pAttachments(attachment);

                int result = vkCreateFramebuffer(renderer.mDevice, createInfo, null, framebuffer);

                if (result != VK_SUCCESS) {
                    throw new RuntimeException(
                            String.format(
                                    "Failed to create framebuffer for %x! Error: %x",
                                    mImageView, -result));
                }

                return framebuffer.get(0);
            }
        }

        /**
         * Update uniform buffer with new data
         *
         * <p>This method will make the object have rotation
         */
        private void updateUniformBuffer(Renderer renderer, float curtime) {
            ubo.model.rotation(curtime * 1.5f, 0.0f, 0.0f, 1.0f);

            try (MemoryStack stack = stackPush()) {
                PointerBuffer pData = stack.pointers(0);
                vkMapMemory(
                        renderer.mDevice, mBuffer.memory, 0, UniformBufferObject.SIZEOF, 0, pData);
                ByteBuffer byteBuffer = pData.getByteBuffer(UniformBufferObject.SIZEOF);
                ubo.copyTo(byteBuffer);
                vkUnmapMemory(renderer.mDevice, mBuffer.memory);
            }
        }

        private void free() {
            vkDestroyFramebuffer(mDevice, framebuffer, null);
            vkDestroyImageView(mDevice, mImageView, null);

            vkDestroyImageView(mDevice, mTextureImageView, null);
            mTextureImage.free();

            mBuffer.free();
        }
    }

    /**
     * Temporary object state
     *
     * <p>This class is purely temporary, it will be removed whenever material system is created,
     * and multiple objects can be rendered from the engine.
     *
     * <p>TODO: Remove this
     */
    private static class TmpObjectState {
        public VulkanPipeline graphics;
        public VulkanBuffer vertexBuffer;
        public VulkanBuffer indexBuffer;

        private static Vertice[] VERTICES = {
            new Vertice(
                    new Vector2f(0.0f, 0.0f),
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    new Vector2f(0.0f, 0.0f)),
            new Vertice(
                    new Vector2f(-0.5f, 0.86603f),
                    new Vector3f(0.0f, 0.0f, 1.0f),
                    new Vector2f(-0.5f, 0.86603f)),
            new Vertice(
                    new Vector2f(0.5f, 0.86603f),
                    new Vector3f(0.0f, 1.0f, 0.0f),
                    new Vector2f(0.5f, 0.86603f)),
            new Vertice(
                    new Vector2f(1.0f, 0.0f),
                    new Vector3f(0.0f, 1.0f, 0.0f),
                    new Vector2f(1.0f, 0.0f)),
            new Vertice(
                    new Vector2f(0.5f, -0.86603f),
                    new Vector3f(0.0f, 1.0f, 0.0f),
                    new Vector2f(0.5f, -0.86603f)),
            new Vertice(
                    new Vector2f(-0.5f, -0.86603f),
                    new Vector3f(0.0f, 0.0f, 1.0f),
                    new Vector2f(-0.5f, -0.86603f)),
            new Vertice(
                    new Vector2f(-1.0f, 0.0f),
                    new Vector3f(0.0f, 0.0f, 1.0f),
                    new Vector2f(-1.0f, 0.0f)),
        };

        private static short[] INDICES = {1, 0, 2, 2, 0, 3, 3, 0, 4, 4, 0, 5, 5, 0, 6, 6, 0, 1};

        /** Create a object state */
        private TmpObjectState(Renderer renderer) {
            Resource<ShaderBuf> vertShader =
                    ShaderBuf.getResource("shader", ShaderKind.VERTEX_SHADER);
            if (vertShader == null) throw new RuntimeException("Failed to load vertex shader!");

            Resource<ShaderBuf> fragShader =
                    ShaderBuf.getResource("shader", ShaderKind.FRAGMENT_SHADER);
            if (fragShader == null) throw new RuntimeException("Failed to load fragment shader!");

            graphics =
                    new VulkanPipeline(
                            vertShader.get(),
                            fragShader.get(),
                            renderer.mDevice,
                            renderer.mExtent,
                            renderer.mDescriptorSetLayout,
                            renderer.mRenderPass);

            vertexBuffer = renderer.createVertexBuffer(VERTICES);
            indexBuffer = renderer.createIndexBuffer(INDICES);
        }

        /** Free resources of the object state */
        private void free() {
            graphics.free();
            vertexBuffer.free();
            indexBuffer.free();
        }
    }

    /**
     * Create a renderer
     *
     * <p>This constructor will create a Vulkan renderer instance, and set everything up so that
     * {@code render} method can be called.
     *
     * @param appName name of the rendered application.
     * @param window handle to GLFW window.
     */
    public Renderer(String appName, long window) throws Exception {
        this.mWindow = window;
        mInstance = createInstance(appName);
        if (DEBUG_MODE) mDebugMessenger = createDebugLogger();
        mSurface = createSurface();
        mPhysicalDevice = pickPhysicalDevice();
        mDevice = createLogicalDevice();
        mGraphicsQueue = createGraphicsQueue();
        mPresentQueue = createPresentQueue();
        mCommandPool = createCommandPool();
        mDescriptorSetLayout = createDescriptorSetLayout();
        mSamplerFactory = new TextureSamplerFactory(mDevice, mPhysicalDevice);
        createSwapchainObjects();
        mFrameContexts = createFrameContexts(FRAMES_IN_FLIGHT);
    }

    /**
     * Render a frame
     *
     * <p>This method will take a list of renderable objects, and render them from the camera point
     * of view.
     *
     * @param camera object from where the renderer should render
     * @param objects list of objects that should be rendered TODO: remove curtime
     */
    public void render(Camera camera, List<Renderable> objects, float curtime) {
        // Right here we will hotload images as they come
        // Also handle vertex, and index buffers

        if (mImageContexts == null) recreateSwapchain();

        try (MemoryStack stack = stackPush()) {
            FrameContext ctx = mFrameContexts[mFrameCounter];
            mFrameCounter = (mFrameCounter + 1) % FRAMES_IN_FLIGHT;

            vkWaitForFences(mDevice, ctx.inFlightFence, true, UINT64_MAX);

            IntBuffer imageIndex = stack.ints(0);
            int res =
                    vkAcquireNextImageKHR(
                            mDevice,
                            mSwapchain,
                            UINT64_MAX,
                            ctx.imageAvailableSemaphore,
                            VK_NULL_HANDLE,
                            imageIndex);
            final ImageContext image = mImageContexts[imageIndex.get(0)];

            if (res == VK_ERROR_OUT_OF_DATE_KHR) {
                recreateSwapchain();
                return;
            }

            if (image.inFlightFence != 0)
                vkWaitForFences(mDevice, image.inFlightFence, true, UINT64_MAX);

            image.inFlightFence = ctx.inFlightFence;

            image.updateUniformBuffer(this, curtime);

            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            LongBuffer waitSemaphores = stack.longs(ctx.imageAvailableSemaphore);
            IntBuffer waitStages = stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            PointerBuffer commandBuffer = stack.pointers(image.commandBuffer);

            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(waitSemaphores);
            submitInfo.pWaitDstStageMask(waitStages);
            submitInfo.pCommandBuffers(commandBuffer);

            LongBuffer signalSemaphores = stack.longs(ctx.renderFinishedSemaphore);
            submitInfo.pSignalSemaphores(signalSemaphores);

            vkResetFences(mDevice, ctx.inFlightFence);

            res = vkQueueSubmit(mGraphicsQueue, submitInfo, ctx.inFlightFence);

            if (res != VK_SUCCESS) {
                vkResetFences(mDevice, ctx.inFlightFence);
                throw new RuntimeException(
                        String.format("Failed to submit draw command buffer! Ret: %x", -res));
            }

            LongBuffer swapchains = stack.longs(mSwapchain);

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.callocStack(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(signalSemaphores);
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(swapchains);
            presentInfo.pImageIndices(imageIndex);

            res = vkQueuePresentKHR(mPresentQueue, presentInfo);

            if (res == VK_ERROR_OUT_OF_DATE_KHR || res == VK_SUBOPTIMAL_KHR) {
                recreateSwapchain();
            } else if (res != VK_SUCCESS) {
                throw new RuntimeException(String.format("Failed to present image! Ret: %x", -res));
            }
        }
    }

    /**
     * Inform the renderer about window being resized
     *
     * <p>This method needs to be called by the app every time the window gets resized, so that the
     * renderer can change its render resolution.
     */
    public void onResize() {
        recreateSwapchain();
    }

    /**
     * Free all renderer resources
     *
     * <p>Call this method to shutdown the renderer and free all its resources.
     */
    @Override
    public void free() {
        vkDeviceWaitIdle(mDevice);
        for (FrameContext frame : mFrameContexts) {
            vkDestroySemaphore(mDevice, frame.renderFinishedSemaphore, null);
            vkDestroySemaphore(mDevice, frame.imageAvailableSemaphore, null);
            vkDestroyFence(mDevice, frame.inFlightFence, null);
        }
        cleanupSwapchain();
        vkDestroyDescriptorSetLayout(mDevice, mDescriptorSetLayout, null);
        mSamplerFactory.free();
        vkDestroyCommandPool(mDevice, mCommandPool, null);
        vkDestroyDevice(mDevice, null);
        destroyDebugMessanger();
        vkDestroySurfaceKHR(mInstance, mSurface, null);
        vkDestroyInstance(mInstance, null);
        glfwDestroyWindow(mWindow);
        glfwTerminate();
    }

    /// Internal code

    /** Recreate swapchain when it becomes invalid */
    private void recreateSwapchain() {
        if (mImageContexts != null) {
            vkQueueWaitIdle(mPresentQueue);
            vkQueueWaitIdle(mGraphicsQueue);
            vkDeviceWaitIdle(mDevice);

            cleanupSwapchain();
        }

        try (MemoryStack stack = stackPush()) {
            IntBuffer x = stack.ints(0);
            IntBuffer y = stack.ints(0);
            glfwGetFramebufferSize(mWindow, x, y);
            LOGGER.info(String.format("%d %d", x.get(0), y.get(0)));
            if (x.get(0) == 0 || y.get(0) == 0) return;
        }

        mPhysicalDevice.onRecreateSwapchain(mSurface);

        createSwapchainObjects();
    }

    /** Cleanup swapchain resources */
    private void cleanupSwapchain() {
        mObjectState.free();
        mObjectState = null;

        Arrays.stream(mImageContexts).forEach(ImageContext::free);

        try (MemoryStack stack = stackPush()) {
            vkFreeCommandBuffers(
                    mDevice,
                    mCommandPool,
                    toPointerBuffer(
                            Arrays.stream(mImageContexts)
                                    .map(d -> d.commandBuffer)
                                    .toArray(VkCommandBuffer[]::new),
                            stack));
        }

        mImageContexts = null;

        vkDestroyDescriptorPool(mDevice, mDescriptorPool, null);
        vkDestroyRenderPass(mDevice, mRenderPass, null);
        vkDestroySwapchainKHR(mDevice, mSwapchain, null);
    }

    /// Internal setup code

    /**
     * Create swapchain objects
     *
     * <p>Create all objects that depend on the swapchain. Unlike initial setup, this method will be
     * called multiple times in situations like window resizes.
     */
    private void createSwapchainObjects() {
        mSurfaceFormat = mPhysicalDevice.swapchainSupport.chooseSurfaceFormat();
        mExtent = mPhysicalDevice.swapchainSupport.chooseExtent(mWindow);
        mSwapchain = createSwapchain();
        mRenderPass = createRenderPass();
        int imageCount = getImageCount();
        mDescriptorPool = createDescriptorPool(imageCount);
        mImageContexts = createImageContexts(imageCount);
        mObjectState = new TmpObjectState(this);
        for (ImageContext img : mImageContexts) {
            img.updateDescriptorSet(this);
            recordCommandBuffer(img, mObjectState);
        }
    }

    /// Instance setup

    /**
     * Create a Vulkan instance
     *
     * <p>Vulkan instance is needed for the duration of the renderer. If debug mode is on, the
     * instance will also enable debug validation layers, which allow to track down issues.
     */
    private VkInstance createInstance(String appName) {
        LOGGER.info("Create instance");

        try (MemoryStack stack = stackPush()) {
            // Prepare basic Vulkan App information
            VkApplicationInfo appInfo = VkApplicationInfo.callocStack(stack);

            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8Safe(appName));
            appInfo.applicationVersion(VK_MAKE_VERSION(0, 0, 1));
            appInfo.pEngineName(stack.UTF8Safe("DragonSkulle Engine"));
            appInfo.engineVersion(VK_MAKE_VERSION(0, 0, 1));
            appInfo.apiVersion(VK_API_VERSION_1_0);

            // Prepare a Vulkan instance information
            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.callocStack(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);
            // set required GLFW extensions
            createInfo.ppEnabledExtensionNames(getExtensions(createInfo, stack));

            // use validation layers if enabled, and setup debugging
            if (DEBUG_MODE) {
                setupDebugValidationLayers(createInfo, stack);
                // setup logging for instance creation
                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = createDebugLoggingInfo(stack);
                createInfo.pNext(debugCreateInfo.address());
            }

            PointerBuffer instancePtr = stack.mallocPointer(1);

            int result = vkCreateInstance(createInfo, null, instancePtr);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create VK instance. Error: %x", -result));
            }

            return new VkInstance(instancePtr.get(0), createInfo);
        }
    }

    /** Returns required extensions for the VK context */
    private PointerBuffer getExtensions(
            VkInstanceCreateInfo createInfoMemoryStack, MemoryStack stack) {
        PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();

        if (DEBUG_MODE) {
            PointerBuffer extensions = stack.mallocPointer(glfwExtensions.capacity() + 1);
            extensions.put(glfwExtensions);
            extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));
            return extensions.rewind();
        } else {
            return glfwExtensions;
        }
    }

    /**
     * Returns validation layers used for debugging
     *
     * <p>Throws if the layers were not available, createInfo gets bound to the data on the stack
     * frame. Do not pop the stack before using up createInfo!!!
     */
    private void setupDebugValidationLayers(VkInstanceCreateInfo createInfo, MemoryStack stack) {
        LOGGER.info("Setup VK validation layers");

        Set<String> wantedSet = new HashSet<>(WANTED_VALIDATION_LAYERS_LIST);

        VkLayerProperties.Buffer properties = getInstanceLayerProperties(stack);

        boolean containsAll =
                wantedSet.isEmpty()
                        || properties.stream()
                                .map(VkLayerProperties::layerNameString)
                                .filter(wantedSet::remove)
                                .anyMatch(__ -> wantedSet.isEmpty());

        if (containsAll) {
            createInfo.ppEnabledLayerNames(toPointerBuffer(WANTED_VALIDATION_LAYERS_LIST, stack));
        } else {
            throw new RuntimeException("Some VK validation layers were not found!");
        }
    }

    /** Utility for converting collection to pointer buffer */
    private PointerBuffer toPointerBuffer(Collection<String> collection, MemoryStack stack) {
        PointerBuffer buffer = stack.mallocPointer(collection.size());
        collection.stream().map(stack::UTF8).forEach(buffer::put);
        return buffer.rewind();
    }

    /** Utility for converting a collection of pointer types to pointer buffer */
    private <T extends Pointer> PointerBuffer toPointerBuffer(T[] array, MemoryStack stack) {
        PointerBuffer buffer = stack.mallocPointer(array.length);
        Arrays.stream(array).forEach(buffer::put);
        return buffer.rewind();
    }

    /** Utility for retrieving instance VkLayerProperties list */
    private VkLayerProperties.Buffer getInstanceLayerProperties(MemoryStack stack) {
        IntBuffer propertyCount = stack.ints(0);
        vkEnumerateInstanceLayerProperties(propertyCount, null);
        VkLayerProperties.Buffer properties =
                VkLayerProperties.mallocStack(propertyCount.get(0), stack);
        vkEnumerateInstanceLayerProperties(propertyCount, properties);
        return properties;
    }

    /** Creates default debug messenger info for logging */
    private VkDebugUtilsMessengerCreateInfoEXT createDebugLoggingInfo(MemoryStack stack) {
        VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo =
                VkDebugUtilsMessengerCreateInfoEXT.callocStack(stack);

        // Initialize debug callback parameters
        debugCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
        debugCreateInfo.messageSeverity(
                VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT
                        | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
                        | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                        | VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT);
        debugCreateInfo.messageType(
                VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
                        | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
                        | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
        debugCreateInfo.pfnUserCallback(Renderer::debugCallback);

        return debugCreateInfo;
    }

    /// Setup debug logging

    /** VK logging entrypoint */
    private static int debugCallback(
            int messageSeverity, int messageType, long pCallbackData, long pUserData) {
        VkDebugUtilsMessengerCallbackDataEXT callbackData =
                VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);

        Level level = Level.FINE;

        if (messageSeverity == VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) {
            level = Level.SEVERE;
        } else if (messageSeverity == VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) {
            level = Level.WARNING;
        } else if (messageSeverity == VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) {
            level = Level.INFO;
        }

        LOGGER.log(level, callbackData.pMessageString());

        return VK_FALSE;
    }

    /** Initializes debugMessenger to receive VK log messages */
    private long createDebugLogger() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pDebugMessenger = stack.longs(0);
            if (vkCreateDebugUtilsMessengerEXT(
                            mInstance, createDebugLoggingInfo(stack), null, pDebugMessenger)
                    != VK_SUCCESS) {
                throw new RuntimeException("Failed to initialize debug messenger");
            }
            return pDebugMessenger.get();
        }
    }

    /// Setup window surface

    /**
     * Create a window surface
     *
     * <p>This method uses window to get its surface that the renderer will draw to
     */
    private long createSurface() {
        LOGGER.info("Create surface");

        try (MemoryStack stack = stackPush()) {
            LongBuffer pSurface = stack.callocLong(1);
            int result = glfwCreateWindowSurface(mInstance, mWindow, null, pSurface);
            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create windows surface! %x", -result));
            }
            return pSurface.get(0);
        }
    }

    /// Physical device setup

    /** Sets up one physical device for use */
    private PhysicalDevice pickPhysicalDevice() {
        LOGGER.info("Pick physical device");
        PhysicalDevice physicalDevice =
                PhysicalDevice.pickPhysicalDevice(
                        mInstance, mSurface, TARGET_GPU, DEVICE_EXTENSIONS);
        if (physicalDevice == null) {
            throw new RuntimeException("Failed to find compatible GPU!");
        }
        LOGGER.info(String.format("Picked GPU: %s", physicalDevice.deviceName));
        return physicalDevice;
    }

    /// Logical device setup

    /** Creates a logical device with required features */
    private VkDevice createLogicalDevice() {
        LOGGER.info("Create logical device");

        try (MemoryStack stack = stackPush()) {
            FloatBuffer queuePriority = stack.floats(1.0f);

            int[] families = mPhysicalDevice.indices.uniqueFamilies();

            VkDeviceQueueCreateInfo.Buffer queueCreateInfo =
                    VkDeviceQueueCreateInfo.callocStack(families.length, stack);

            IntStream.range(0, families.length)
                    .forEach(
                            i -> {
                                queueCreateInfo
                                        .get(i)
                                        .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                                queueCreateInfo.get(i).queueFamilyIndex(families[i]);
                                queueCreateInfo.get(i).pQueuePriorities(queuePriority);
                            });

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.callocStack(stack);
            deviceFeatures.samplerAnisotropy(mPhysicalDevice.featureSupport.anisotropyEnable);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.callocStack(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfo);
            createInfo.pEnabledFeatures(deviceFeatures);
            createInfo.ppEnabledExtensionNames(toPointerBuffer(DEVICE_EXTENSIONS, stack));

            if (DEBUG_MODE)
                createInfo.ppEnabledLayerNames(
                        toPointerBuffer(WANTED_VALIDATION_LAYERS_LIST, stack));

            PointerBuffer pDevice = stack.callocPointer(1);

            int result = vkCreateDevice(mPhysicalDevice.device, createInfo, null, pDevice);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create VK logical device! Err: %x", -result));
            }

            VkDevice device = new VkDevice(pDevice.get(0), mPhysicalDevice.device, createInfo);

            return device;
        }
    }

    /// Queue setup

    /**
     * Create a graphics queue
     *
     * <p>This queue is used to submit graphics commands every frame
     */
    private VkQueue createGraphicsQueue() {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pQueue = stack.callocPointer(1);
            vkGetDeviceQueue(mDevice, mPhysicalDevice.indices.graphicsFamily, 0, pQueue);
            return new VkQueue(pQueue.get(0), mDevice);
        }
    }

    /**
     * Create a presentation queue
     *
     * <p>This queue is used to display rendered frames on the screen
     */
    private VkQueue createPresentQueue() {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pQueue = stack.callocPointer(1);
            vkGetDeviceQueue(mDevice, mPhysicalDevice.indices.presentFamily, 0, pQueue);
            return new VkQueue(pQueue.get(0), mDevice);
        }
    }

    /// Command pool setup

    /**
     * Create a command pool
     *
     * <p>This method creates a command pool which is used for creating command buffers.
     */
    private long createCommandPool() {
        LOGGER.info("Create command pool");

        try (MemoryStack stack = stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.callocStack(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(mPhysicalDevice.indices.graphicsFamily);

            LongBuffer pCommandPool = stack.longs(0);

            int result = vkCreateCommandPool(mDevice, poolInfo, null, pCommandPool);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create command pool! Err: %x", -result));
            }

            return pCommandPool.get(0);
        }
    }

    /// Descriptor set setup

    /**
     * Create a descriptor set layout
     *
     * <p>This layout is used in creating descriptor sets. It describes the properties shaders have
     * in different stages.
     *
     * <p>TODO: probably move to material system
     */
    private long createDescriptorSetLayout() {
        LOGGER.info("Create descriptor set layout");

        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer layoutBindings =
                    VkDescriptorSetLayoutBinding.callocStack(2, stack);
            VkDescriptorSetLayoutBinding uboLayoutBinding = layoutBindings.get(0);
            uboLayoutBinding.binding(0);
            uboLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            uboLayoutBinding.descriptorCount(1);
            uboLayoutBinding.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

            VkDescriptorSetLayoutBinding samplerLayoutBinding = layoutBindings.get(1);
            samplerLayoutBinding.binding(1);
            samplerLayoutBinding.descriptorCount(1);
            samplerLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            samplerLayoutBinding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

            VkDescriptorSetLayoutCreateInfo layoutInfo =
                    VkDescriptorSetLayoutCreateInfo.callocStack(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.pBindings(layoutBindings);

            LongBuffer pDescriptorSetLayout = stack.longs(0);

            int res = vkCreateDescriptorSetLayout(mDevice, layoutInfo, null, pDescriptorSetLayout);

            if (res != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create descriptor set layout! Res: %x", -res));
            }

            return pDescriptorSetLayout.get(0);
        }
    }

    /// Swapchain setup

    /** Sets up the swapchain required for rendering */
    private long createSwapchain() {
        LOGGER.info("Setup swapchain");

        try (MemoryStack stack = stackPush()) {
            int presentMode = mPhysicalDevice.swapchainSupport.choosePresentMode();
            int imageCount = mPhysicalDevice.swapchainSupport.chooseImageCount();

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.callocStack(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(mSurface);
            createInfo.minImageCount(imageCount);
            createInfo.imageFormat(mSurfaceFormat.format());
            createInfo.imageColorSpace(mSurfaceFormat.colorSpace());
            createInfo.imageExtent(mExtent);
            createInfo.imageArrayLayers(1);
            // Render directly. For post-processing,
            // we may need VK_IMAGE_USAGE_TRANSFER_DST_BIT
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            // If we have separate queues, use concurrent mode which is easier to work with,
            // although slightly less efficient.
            if (mGraphicsQueue.address() != mPresentQueue.address()) {
                IntBuffer indices =
                        stack.ints(
                                mPhysicalDevice.indices.graphicsFamily,
                                mPhysicalDevice.indices.presentFamily);
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                createInfo.pQueueFamilyIndices(indices);
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            createInfo.preTransform(
                    mPhysicalDevice.swapchainSupport.capabilities.currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);

            LongBuffer pSwapchain = stack.longs(0);

            int result = vkCreateSwapchainKHR(mDevice, createInfo, null, pSwapchain);

            if (result != VK_SUCCESS)
                throw new RuntimeException(
                        String.format("Failed to create swapchain! Error: %x", -result));

            return pSwapchain.get(0);
        }
    }

    /// Per swapchain image setup

    /** Get the number of images swapchain was created with */
    private int getImageCount() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pImageCount = stack.ints(0);

            vkGetSwapchainImagesKHR(mDevice, mSwapchain, pImageCount, null);

            return pImageCount.get(0);
        }
    }

    /** Create a context for each swapchain image */
    private ImageContext[] createImageContexts(int imageCount) {
        try (MemoryStack stack = stackPush()) {
            // Allocate swapchain images
            LongBuffer pSwapchainImages = stack.mallocLong(imageCount);

            IntBuffer pImageCount = stack.ints(imageCount);

            vkGetSwapchainImagesKHR(mDevice, mSwapchain, pImageCount, pSwapchainImages);

            // Allocate descriptor sets
            LongBuffer setLayouts = stack.mallocLong(imageCount);
            IntStream.range(0, imageCount)
                    .mapToLong(__ -> mDescriptorSetLayout)
                    .forEach(setLayouts::put);
            setLayouts.rewind();

            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
            allocInfo.descriptorPool(mDescriptorPool);
            allocInfo.pSetLayouts(setLayouts);

            LongBuffer pDescriptorSets = stack.mallocLong(imageCount);

            int res = vkAllocateDescriptorSets(mDevice, allocInfo, pDescriptorSets);

            if (res != VK_SUCCESS)
                throw new RuntimeException(
                        String.format("Failed to create descriptor sets! Res: %x", -res));

            // Allocate command buffers

            VkCommandBufferAllocateInfo cmdAllocInfo =
                    VkCommandBufferAllocateInfo.callocStack(stack);
            cmdAllocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            cmdAllocInfo.commandPool(mCommandPool);
            cmdAllocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            cmdAllocInfo.commandBufferCount(imageCount);

            PointerBuffer buffers = stack.mallocPointer(cmdAllocInfo.commandBufferCount());

            int result = vkAllocateCommandBuffers(mDevice, cmdAllocInfo, buffers);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create command buffers! Err: %x", -result));
            }

            return IntStream.range(0, imageCount)
                    .mapToObj(
                            i ->
                                    new ImageContext(
                                            this,
                                            pSwapchainImages.get(i),
                                            pDescriptorSets.get(i),
                                            buffers.get(i)))
                    .toArray(ImageContext[]::new);
        }
    }

    /// Descriptor pool setup

    /**
     * Create a descriptor pool
     *
     * <p>This pool is used for creating descriptor sets.
     */
    private long createDescriptorPool(int imageCount) {
        LOGGER.info("Setup descriptor pool");

        try (MemoryStack stack = stackPush()) {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.callocStack(2, stack);
            poolSizes.get(0).type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            poolSizes.get(0).descriptorCount(imageCount);
            poolSizes.get(1).type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            poolSizes.get(1).descriptorCount(imageCount);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.callocStack(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(imageCount);

            LongBuffer pDescriptorPool = stack.longs(0);

            int res = vkCreateDescriptorPool(mDevice, poolInfo, null, pDescriptorPool);

            if (res != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create descriptor pool! Res: %x", -res));
            }

            return pDescriptorPool.get(0);
        }
    }

    /// Image view setup

    /** Creates a image view for an image */
    private long createImageView(long image) {
        return createImageView(image, mSurfaceFormat.format());
    }

    /**
     * Creates a image view for an image
     *
     * <p>Image views are needed to render to/read from.
     */
    private long createImageView(long image, int format) {
        try (MemoryStack stack = stackPush()) {
            VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.callocStack(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            createInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
            createInfo.format(format);

            createInfo.components().r(VK_COMPONENT_SWIZZLE_IDENTITY);
            createInfo.components().g(VK_COMPONENT_SWIZZLE_IDENTITY);
            createInfo.components().b(VK_COMPONENT_SWIZZLE_IDENTITY);
            createInfo.components().a(VK_COMPONENT_SWIZZLE_IDENTITY);

            createInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            createInfo.subresourceRange().baseMipLevel(0);
            createInfo.subresourceRange().levelCount(1);
            createInfo.subresourceRange().baseArrayLayer(0);
            createInfo.subresourceRange().layerCount(1);

            createInfo.image(image);

            LongBuffer imageView = stack.longs(0);

            int result = vkCreateImageView(mDevice, createInfo, null, imageView);
            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format(
                                "Failed to create image view for %x! Error: %x", image, -result));
            }

            return imageView.get(0);
        }
    }

    /// Render pass setup

    /**
     * Create a render pass
     *
     * <p>This method will describe how a single render pass should behave.
     *
     * <p>TODO: move to material system? Move to render pass manager?
     */
    private long createRenderPass() {
        LOGGER.info("Create render pass");

        try (MemoryStack stack = stackPush()) {
            VkAttachmentDescription.Buffer colorAttachment =
                    VkAttachmentDescription.callocStack(1, stack);
            colorAttachment.format(mSurfaceFormat.format());
            colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            // We don't use stencils yet
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            // We present the image after rendering, and don't care what it was,
            // since we clear it anyways.
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            VkAttachmentReference.Buffer colorAttachmentRef =
                    VkAttachmentReference.callocStack(1, stack);
            colorAttachmentRef.attachment(0);
            colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.callocStack(1, stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(colorAttachmentRef);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.callocStack(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(colorAttachment);
            renderPassInfo.pSubpasses(subpass);

            // Make render passes wait for COLOR_ATTACHMENT_OUTPUT stage
            VkSubpassDependency.Buffer dependency = VkSubpassDependency.callocStack(1, stack);
            dependency.srcSubpass(VK_SUBPASS_EXTERNAL);
            dependency.dstSubpass(0);
            dependency.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependency.srcAccessMask(0);
            dependency.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependency.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

            renderPassInfo.pDependencies(dependency);

            LongBuffer pRenderPass = stack.longs(0);

            int result = vkCreateRenderPass(mDevice, renderPassInfo, null, pRenderPass);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create render pass! Err: %x", -result));
            }

            return pRenderPass.get(0);
        }
    }

    /// Vertex and index buffers

    /**
     * Create a vertex buffer
     *
     * <p>As the name implies, this buffer holds vertices
     */
    private VulkanBuffer createVertexBuffer(Vertice[] vertices) {
        LOGGER.info("Create vertex buffer");

        try (MemoryStack stack = stackPush()) {

            long size = vertices.length * Vertice.SIZEOF;

            VulkanBuffer stagingBuffer =
                    new VulkanBuffer(
                            mDevice,
                            mPhysicalDevice,
                            size,
                            VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                    | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

            PointerBuffer pData = stack.pointers(0);
            vkMapMemory(mDevice, stagingBuffer.memory, 0, size, 0, pData);
            ByteBuffer byteBuffer = pData.getByteBuffer((int) size);
            for (Vertice v : vertices) {
                v.copyTo(byteBuffer);
            }
            vkUnmapMemory(mDevice, stagingBuffer.memory);

            VulkanBuffer vertexBuffer =
                    new VulkanBuffer(
                            mDevice,
                            mPhysicalDevice,
                            size,
                            VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            VkCommandBuffer commandBuffer = beginSingleUseCommandBuffer();
            stagingBuffer.copyTo(commandBuffer, vertexBuffer, size);
            endSingleUseCommandBuffer(commandBuffer);

            stagingBuffer.free();

            return vertexBuffer;
        }
    }

    /**
     * Create index buffer
     *
     * <p>This buffer holds indices of the vertices to render in multiples of 3.
     */
    private VulkanBuffer createIndexBuffer(short[] indices) {
        LOGGER.info("Setup index buffer");

        try (MemoryStack stack = stackPush()) {

            long size = indices.length * 2;

            VulkanBuffer stagingBuffer =
                    new VulkanBuffer(
                            mDevice,
                            mPhysicalDevice,
                            size,
                            VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                    | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

            PointerBuffer pData = stack.pointers(0);
            vkMapMemory(mDevice, stagingBuffer.memory, 0, size, 0, pData);
            ByteBuffer byteBuffer = pData.getByteBuffer((int) size);
            for (short i : indices) {
                byteBuffer.putShort(i);
            }
            vkUnmapMemory(mDevice, stagingBuffer.memory);

            VulkanBuffer indexBuffer =
                    new VulkanBuffer(
                            mDevice,
                            mPhysicalDevice,
                            size,
                            VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            VkCommandBuffer commandBuffer = beginSingleUseCommandBuffer();
            stagingBuffer.copyTo(commandBuffer, indexBuffer, size);
            endSingleUseCommandBuffer(commandBuffer);

            stagingBuffer.free();

            return indexBuffer;
        }
    }

    /**
     * Create a single use command buffer.
     *
     * <p>This command buffer can be flushed with {@code endSingleUseCommandBuffer}
     */
    private VkCommandBuffer beginSingleUseCommandBuffer() {
        try (MemoryStack stack = stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandPool(mCommandPool);
            allocInfo.commandBufferCount(1);

            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            vkAllocateCommandBuffers(mDevice, allocInfo, pCommandBuffer);

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommandBuffer.get(0), mDevice);

            vkBeginCommandBuffer(commandBuffer, beginInfo);

            return commandBuffer;
        }
    }

    /** Ends and frees the single use command buffer */
    private void endSingleUseCommandBuffer(VkCommandBuffer commandBuffer) {
        try (MemoryStack stack = stackPush()) {
            vkEndCommandBuffer(commandBuffer);

            PointerBuffer pCommandBuffer = stack.pointers(commandBuffer.address());

            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(pCommandBuffer);

            vkQueueSubmit(mGraphicsQueue, submitInfo, NULL);
            vkQueueWaitIdle(mGraphicsQueue);

            vkFreeCommandBuffers(mDevice, mCommandPool, pCommandBuffer);
        }
    }

    /// Create a graphics pipeline

    /// Frame Context setup

    private FrameContext[] createFrameContexts(int framesInFlight) {
        LOGGER.info("Setup sync objects");

        try (MemoryStack stack = stackPush()) {
            FrameContext[] frames = new FrameContext[framesInFlight];

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.callocStack(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.callocStack(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            IntStream.range(0, framesInFlight)
                    .forEach(
                            i -> {
                                FrameContext ctx = new FrameContext();

                                LongBuffer pSem1 = stack.longs(0);
                                LongBuffer pSem2 = stack.longs(0);
                                LongBuffer pFence = stack.longs(0);

                                int res1 = vkCreateSemaphore(mDevice, semaphoreInfo, null, pSem1);
                                int res2 = vkCreateSemaphore(mDevice, semaphoreInfo, null, pSem2);
                                int res3 = vkCreateFence(mDevice, fenceInfo, null, pFence);

                                if (res1 != VK_SUCCESS
                                        || res2 != VK_SUCCESS
                                        || res3 != VK_SUCCESS) {
                                    throw new RuntimeException(
                                            String.format(
                                                    "Failed to create semaphores! Err: %x %x",
                                                    -res1, -res2, -res2));
                                }

                                ctx.imageAvailableSemaphore = pSem1.get(0);
                                ctx.renderFinishedSemaphore = pSem2.get(0);
                                ctx.inFlightFence = pFence.get(0);

                                frames[i] = ctx;
                            });
            return frames;
        }
    }

    /// Record command buffer to temporary object

    public void recordCommandBuffer(ImageContext ctx, TmpObjectState state) {
        try (MemoryStack stack = stackPush()) {
            // Record the command buffers
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.callocStack(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
            renderPassInfo.renderPass(mRenderPass);

            renderPassInfo.renderArea().offset().clear();
            renderPassInfo.renderArea().extent(mExtent);

            VkClearValue.Buffer clearColor = VkClearValue.callocStack(1, stack);
            renderPassInfo.pClearValues(clearColor);

            int res = vkBeginCommandBuffer(ctx.commandBuffer, beginInfo);

            if (res != VK_SUCCESS) {
                String format =
                        String.format("Failed to begin recording command buffer! Err: %x", res);
                throw new RuntimeException(format);
            }

            renderPassInfo.framebuffer(ctx.framebuffer);

            // This is the beginning :)
            vkCmdBeginRenderPass(ctx.commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

            vkCmdBindPipeline(
                    ctx.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, state.graphics.pipeline);

            LongBuffer vertexBuffers = stack.longs(state.vertexBuffer.buffer);
            LongBuffer offsets = stack.longs(0);
            vkCmdBindVertexBuffers(ctx.commandBuffer, 0, vertexBuffers, offsets);
            vkCmdBindIndexBuffer(
                    ctx.commandBuffer, state.indexBuffer.buffer, 0, VK_INDEX_TYPE_UINT16);

            LongBuffer descriptorSet = stack.longs(ctx.descriptorSet);
            vkCmdBindDescriptorSets(
                    ctx.commandBuffer,
                    VK_PIPELINE_BIND_POINT_GRAPHICS,
                    state.graphics.layout,
                    0,
                    descriptorSet,
                    null);

            vkCmdDrawIndexed(ctx.commandBuffer, TmpObjectState.INDICES.length, 1, 0, 0, 0);

            vkCmdEndRenderPass(ctx.commandBuffer);

            // And this is the end
            res = vkEndCommandBuffer(ctx.commandBuffer);

            if (res != VK_SUCCESS) {
                String format =
                        String.format("Failed to end recording command buffer! Err: %x", res);
                throw new RuntimeException(format);
            }
        }
    }

    /// Setup texture

    /// Cleanup code

    /** Destroys debugMessenger if exists */
    private void destroyDebugMessanger() {
        if (mDebugMessenger == 0) return;
        vkDestroyDebugUtilsMessengerEXT(mInstance, mDebugMessenger, null);
    }
}
