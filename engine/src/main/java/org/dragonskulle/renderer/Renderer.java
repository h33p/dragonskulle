/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static java.util.stream.Collectors.toSet;
import static org.dragonskulle.utils.Env.envBool;
import static org.dragonskulle.utils.Env.envInt;
import static org.dragonskulle.utils.Env.envString;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.vkCreateDebugUtilsMessengerEXT;
import static org.lwjgl.vulkan.EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkCreateSwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_API_VERSION_1_0;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_CLEAR;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_STORE;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_FALSE;
import static org.lwjgl.vulkan.VK10.VK_FENCE_CREATE_SIGNALED_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_INDEX_TYPE_UINT32;
import static org.lwjgl.vulkan.VK10.VK_MAKE_VERSION;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_CONCURRENT;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUBPASS_CONTENTS_INLINE;
import static org.lwjgl.vulkan.VK10.VK_SUBPASS_EXTERNAL;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkBeginCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdBeginRenderPass;
import static org.lwjgl.vulkan.VK10.vkCmdBindDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCmdBindIndexBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdBindPipeline;
import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK10.vkCmdDrawIndexed;
import static org.lwjgl.vulkan.VK10.vkCmdEndRenderPass;
import static org.lwjgl.vulkan.VK10.vkCmdPushConstants;
import static org.lwjgl.vulkan.VK10.vkCreateCommandPool;
import static org.lwjgl.vulkan.VK10.vkCreateDevice;
import static org.lwjgl.vulkan.VK10.vkCreateFence;
import static org.lwjgl.vulkan.VK10.vkCreateFramebuffer;
import static org.lwjgl.vulkan.VK10.vkCreateInstance;
import static org.lwjgl.vulkan.VK10.vkCreateRenderPass;
import static org.lwjgl.vulkan.VK10.vkCreateSemaphore;
import static org.lwjgl.vulkan.VK10.vkDestroyCommandPool;
import static org.lwjgl.vulkan.VK10.vkDestroyDevice;
import static org.lwjgl.vulkan.VK10.vkDestroyFence;
import static org.lwjgl.vulkan.VK10.vkDestroyFramebuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyImageView;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;
import static org.lwjgl.vulkan.VK10.vkDestroyRenderPass;
import static org.lwjgl.vulkan.VK10.vkDestroySemaphore;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;
import static org.lwjgl.vulkan.VK10.vkEndCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkEnumerateInstanceLayerProperties;
import static org.lwjgl.vulkan.VK10.vkFreeCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkGetDeviceQueue;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;
import static org.lwjgl.vulkan.VK10.vkQueueWaitIdle;
import static org.lwjgl.vulkan.VK10.vkResetFences;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;
import static org.lwjgl.vulkan.VK10.vkWaitForFences;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.renderer.DrawCallState.DrawData;
import org.dragonskulle.renderer.DrawCallState.NonInstancedDraw;
import org.dragonskulle.renderer.components.Camera;
import org.dragonskulle.renderer.components.Light;
import org.dragonskulle.renderer.components.Renderable;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkClearDepthStencilValue;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

/**
 * Vulkan renderer.
 *
 * @author Aurimas Blažulionis
 *     <p>This renderer allows to draw {@code Renderable} objects on screen. Application needs to
 *     call {@code onResized} when its window gets resized.
 *     <p>This renderer was originally based on<a href="https://vulkan-tutorial.com/">Vulkan
 *     Tutorial</a>, and was later rewritten with a much more manageable design.
 */
@Accessors(prefix = "m")
@Getter(AccessLevel.PACKAGE)
@Log
public class Renderer implements NativeResource {

    private long mWindow;

    private VkInstance mInstance;
    private long mDebugMessenger;
    private long mSurface;

    private PhysicalDevice mPhysicalDevice;
    private int mMSAASamples = VK_SAMPLE_COUNT_1_BIT;

    private VkDevice mDevice;
    private VkQueue mGraphicsQueue;
    private VkQueue mPresentQueue;

    private long mCommandPool;

    private TextureSamplerFactory mSamplerFactory;
    private VulkanSampledTextureFactory mTextureFactory;
    private TextureSetLayoutFactory mTextureSetLayoutFactory;
    private TextureSetFactory mTextureSetFactory;
    private VertexConstants mVertexConstants = new VertexConstants();

    private VkSurfaceFormatKHR mSurfaceFormat;
    private VkExtent2D mExtent;
    private long mSwapchain;
    private long mRenderPass;

    private ImageContext[] mImageContexts;
    private FrameContext[] mFrameContexts;

    @Getter(AccessLevel.PUBLIC)
    private int mInstanceBufferSize = 0;

    private VulkanImage mColorImage;
    private long mColorImageView;

    private VulkanImage mDepthImage;
    private long mDepthImageView;

    private int mFrameCounter = 0;

    @Getter(AccessLevel.PUBLIC)
    private int mInstancedCalls = 0;

    @Getter(AccessLevel.PUBLIC)
    private int mSlowCalls = 0;

    private VulkanMeshBuffer mCurrentMeshBuffer;
    private Map<Integer, VulkanMeshBuffer> mDiscardedMeshBuffers = new HashMap<>();
    private Map<Integer, Map<DrawCallState.HashKey, DrawCallState>> mDrawInstances =
            new TreeMap<>();

    private static final List<String> WANTED_VALIDATION_LAYERS_LIST =
            Arrays.asList("VK_LAYER_KHRONOS_validation");
    private static final Set<String> DEVICE_EXTENSIONS =
            Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME).collect(toSet());

    static final boolean DEBUG_MODE = envBool("DEBUG_RENDERER", false);
    private static final String TARGET_GPU = envString("TARGET_GPU", null);

    private static final int INSTANCE_BUFFER_SIZE = envInt("INSTANCE_BUFFER_SIZE", 1);

    private static final int MSAA_SAMPLES = envInt("MSAA_SAMPLES", 4);

    private static final long UINT64_MAX = -1L;
    private static final int FRAMES_IN_FLIGHT = 4;

    /** Synchronization objects for when multiple frames are rendered at a time. */
    private class FrameContext {
        public long mImageAvailableSemaphore;
        public long mRenderFinishedSemaphore;
        public long mInFlightFence;
    }

    /** All state for a single frame. */
    private static class ImageContext {
        public VkCommandBuffer mCommandBuffer;
        public long mInFlightFence;
        public long mFramebuffer;
        public int mImageIndex;

        public int mInstanceBufferSize;
        public VulkanBuffer mInstanceBuffer;

        private long mImageView;
        private VkDevice mDevice;

        /** Create a image context. */
        private ImageContext(
                Renderer renderer, VulkanImage image, int imageIndex, long commandBuffer) {
            this.mCommandBuffer = new VkCommandBuffer(commandBuffer, renderer.mDevice);
            this.mImageView = image.createImageView();
            this.mFramebuffer = createFramebuffer(renderer);
            this.mImageIndex = imageIndex;

            this.mDevice = renderer.mDevice;

            mInstanceBufferSize = 0;
        }

        /** Create a framebuffer from image view. */
        private long createFramebuffer(Renderer renderer) {
            try (MemoryStack stack = stackPush()) {
                VkFramebufferCreateInfo createInfo = VkFramebufferCreateInfo.callocStack(stack);
                createInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);

                createInfo.renderPass(renderer.mRenderPass);
                createInfo.width(renderer.mExtent.width());
                createInfo.height(renderer.mExtent.height());
                createInfo.layers(1);

                LongBuffer attachments =
                        stack.longs(renderer.mColorImageView, renderer.mDepthImageView, mImageView);
                LongBuffer framebuffer = stack.longs(0);

                createInfo.pAttachments(attachments);

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

        private void free() {
            if (mInstanceBuffer != null) {
                mInstanceBuffer.free();
            }
            vkDestroyFramebuffer(mDevice, mFramebuffer, null);
            vkDestroyImageView(mDevice, mImageView, null);
        }
    }

    /**
     * Create a renderer.
     *
     * <p>This constructor will create a Vulkan renderer instance, and set everything up so that
     * {@code render} method can be called.
     *
     * @param appName name of the rendered application.
     * @param window handle to GLFW window.
     * @throws RuntimeException when initialization fails.
     */
    public Renderer(String appName, long window) throws RuntimeException {
        log.info("Initialize renderer");
        mInstanceBufferSize = INSTANCE_BUFFER_SIZE * 4096;
        this.mWindow = window;
        mInstance = createInstance(appName);
        if (DEBUG_MODE) {
            mDebugMessenger = createDebugLogger();
        }
        mSurface = createSurface();
        mPhysicalDevice = pickPhysicalDevice();
        mMSAASamples = mPhysicalDevice.findSuitableMSAACount(MSAA_SAMPLES);
        mDevice = createLogicalDevice();
        mGraphicsQueue = createGraphicsQueue();
        mPresentQueue = createPresentQueue();
        mCommandPool = createCommandPool();
        mSamplerFactory = new TextureSamplerFactory(mDevice, mPhysicalDevice);
        mTextureFactory =
                new VulkanSampledTextureFactory(
                        mDevice, mPhysicalDevice, mCommandPool, mGraphicsQueue, mSamplerFactory);
        mTextureSetLayoutFactory = new TextureSetLayoutFactory(mDevice);
        mCurrentMeshBuffer = new VulkanMeshBuffer(mDevice, mPhysicalDevice);
        createSwapchainObjects();
        mFrameContexts = createFrameContexts(FRAMES_IN_FLIGHT);
    }

    /**
     * Render a frame.
     *
     * <p>This method will take a list of renderable objects, and render them from the camera point
     * of view.
     *
     * @param camera object from where the renderer should render
     * @param objects list of objects that should be rendered
     * @param lights list of lights to light the objects with
     */
    public void render(Camera camera, List<Renderable> objects, List<Light> lights) {
        if (mImageContexts == null) {
            recreateSwapchain();
        }

        if (mImageContexts == null) {
            return;
        }

        camera.updateAspectRatio(mExtent.width(), mExtent.height());

        try (MemoryStack stack = stackPush()) {
            FrameContext ctx = mFrameContexts[mFrameCounter];
            mFrameCounter = (mFrameCounter + 1) % FRAMES_IN_FLIGHT;

            vkWaitForFences(mDevice, ctx.mInFlightFence, true, UINT64_MAX);

            IntBuffer pImageIndex = stack.ints(0);
            int res =
                    vkAcquireNextImageKHR(
                            mDevice,
                            mSwapchain,
                            UINT64_MAX,
                            ctx.mImageAvailableSemaphore,
                            VK_NULL_HANDLE,
                            pImageIndex);
            final int imageIndex = pImageIndex.get(0);
            final ImageContext image = mImageContexts[imageIndex];

            if (res == VK_ERROR_OUT_OF_DATE_KHR) {
                recreateSwapchain();
                return;
            }

            if (image.mInFlightFence != 0) {
                vkWaitForFences(mDevice, image.mInFlightFence, true, UINT64_MAX);
            }

            image.mInFlightFence = ctx.mInFlightFence;

            VulkanMeshBuffer discardedBuffer = mDiscardedMeshBuffers.remove(imageIndex);

            if (discardedBuffer != null) {
                discardedBuffer.free();
            }

            updateInstanceBuffer(image, objects, lights);
            recordCommandBuffer(image, camera);

            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            LongBuffer waitSemaphores = stack.longs(ctx.mImageAvailableSemaphore);
            IntBuffer waitStages = stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            PointerBuffer commandBuffer = stack.pointers(image.mCommandBuffer);

            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(waitSemaphores);
            submitInfo.pWaitDstStageMask(waitStages);
            submitInfo.pCommandBuffers(commandBuffer);

            LongBuffer signalSemaphores = stack.longs(ctx.mRenderFinishedSemaphore);
            submitInfo.pSignalSemaphores(signalSemaphores);

            vkResetFences(mDevice, ctx.mInFlightFence);

            res = vkQueueSubmit(mGraphicsQueue, submitInfo, ctx.mInFlightFence);

            if (res != VK_SUCCESS) {
                vkResetFences(mDevice, ctx.mInFlightFence);
                throw new RuntimeException(
                        String.format("Failed to submit draw command buffer! Ret: %x", -res));
            }

            LongBuffer swapchains = stack.longs(mSwapchain);

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.callocStack(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(signalSemaphores);
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(swapchains);
            presentInfo.pImageIndices(pImageIndex);

            res = vkQueuePresentKHR(mPresentQueue, presentInfo);

            if (res == VK_ERROR_OUT_OF_DATE_KHR || res == VK_SUBOPTIMAL_KHR) {
                recreateSwapchain();
            } else if (res != VK_SUCCESS) {
                throw new RuntimeException(String.format("Failed to present image! Ret: %x", -res));
            }
        }
    }

    /**
     * Inform the renderer about window being resized.
     *
     * <p>This method needs to be called by the app every time the window gets resized, so that the
     * renderer can change its render resolution.
     */
    public void onResize() {
        recreateSwapchain();
    }

    /**
     * Retrieve the size of the current vertex buffer.
     *
     * @return size of vertex buffer
     */
    public int getVertexBufferSize() {
        return mCurrentMeshBuffer == null ? 0 : mCurrentMeshBuffer.getMaxVertexOffset();
    }

    /**
     * Retrieve the size of the current index buffer.
     *
     * @return size of index buffer
     */
    public int getIndexBufferSize() {
        return mCurrentMeshBuffer == null ? 0 : mCurrentMeshBuffer.getMaxIndexOffset();
    }

    /**
     * Free all renderer resources.
     *
     * <p>Call this method to shutdown the renderer and free all its resources.
     */
    @Override
    public void free() {
        vkDeviceWaitIdle(mDevice);
        for (FrameContext frame : mFrameContexts) {
            vkDestroySemaphore(mDevice, frame.mRenderFinishedSemaphore, null);
            vkDestroySemaphore(mDevice, frame.mImageAvailableSemaphore, null);
            vkDestroyFence(mDevice, frame.mInFlightFence, null);
        }
        cleanupSwapchain();
        mCurrentMeshBuffer.free();
        mTextureSetLayoutFactory.free();
        mTextureFactory.free();
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

    /** Recreate swapchain when it becomes invalid. */
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
            log.finer(String.format("%d %d", x.get(0), y.get(0)));
            if (x.get(0) == 0 || y.get(0) == 0) {
                return;
            }
        }

        mPhysicalDevice.onRecreateSwapchain(mSurface);

        createSwapchainObjects();
    }

    /** Cleanup swapchain resources. */
    private void cleanupSwapchain() {
        Arrays.stream(mImageContexts).forEach(ImageContext::free);

        try (MemoryStack stack = stackPush()) {
            vkFreeCommandBuffers(
                    mDevice,
                    mCommandPool,
                    toPointerBuffer(
                            Arrays.stream(mImageContexts)
                                    .map(d -> d.mCommandBuffer)
                                    .toArray(VkCommandBuffer[]::new),
                            stack));
        }

        mImageContexts = null;

        for (Map<DrawCallState.HashKey, DrawCallState> stateMap : mDrawInstances.values()) {
            for (DrawCallState state : stateMap.values()) {
                state.free();
            }
        }
        mDrawInstances.clear();

        for (VulkanMeshBuffer meshBuffer : mDiscardedMeshBuffers.values()) {
            meshBuffer.free();
        }
        mDiscardedMeshBuffers.clear();

        mTextureSetFactory.free();
        mTextureSetFactory = null;

        vkDestroyRenderPass(mDevice, mRenderPass, null);

        vkDestroyImageView(mDevice, mDepthImageView, null);
        mDepthImageView = 0;

        mDepthImage.free();
        mDepthImage = null;

        vkDestroyImageView(mDevice, mColorImageView, null);
        mColorImageView = 0;

        mColorImage.free();
        mColorImage = null;

        vkDestroySwapchainKHR(mDevice, mSwapchain, null);
    }

    /// Internal setup code.

    /**
     * Create swapchain objects
     *
     * <p>Create all objects that depend on the swapchain. Unlike initial setup, this method will be
     * called multiple times in situations like window resizes.
     */
    private void createSwapchainObjects() {
        mSurfaceFormat = mPhysicalDevice.getSwapchainSupport().chooseSurfaceFormat();
        mExtent = mPhysicalDevice.getSwapchainSupport().chooseExtent(mWindow);
        mSwapchain = createSwapchain();
        mRenderPass = createRenderPass();
        mColorImage = createColorImage();
        mColorImageView = mColorImage.createImageView();
        mDepthImage = createDepthImage();
        mDepthImageView = mDepthImage.createImageView();
        int imageCount = getImageCount();
        mImageContexts = createImageContexts(imageCount);
        mTextureSetFactory = new TextureSetFactory(mDevice, mTextureSetLayoutFactory, imageCount);
    }

    /// Instance setup

    /**
     * Create a Vulkan instance.
     *
     * <p>Vulkan instance is needed for the duration of the renderer. If debug mode is on, the
     * instance will also enable debug validation layers, which allow to track down issues.
     */
    private VkInstance createInstance(String appName) {
        log.fine("Create instance");

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

    /** Returns required extensions for the VK context. */
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
        log.fine("Setup VK validation layers");

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

    /** Utility for converting collection to pointer buffer. */
    private PointerBuffer toPointerBuffer(Collection<String> collection, MemoryStack stack) {
        PointerBuffer buffer = stack.mallocPointer(collection.size());
        collection.stream().map(stack::UTF8).forEach(buffer::put);
        return buffer.rewind();
    }

    /** Utility for converting a collection of pointer types to pointer buffer. */
    private <T extends Pointer> PointerBuffer toPointerBuffer(T[] array, MemoryStack stack) {
        PointerBuffer buffer = stack.mallocPointer(array.length);
        Arrays.stream(array).forEach(buffer::put);
        return buffer.rewind();
    }

    /** Utility for retrieving instance VkLayerProperties list. */
    private VkLayerProperties.Buffer getInstanceLayerProperties(MemoryStack stack) {
        IntBuffer propertyCount = stack.ints(0);
        vkEnumerateInstanceLayerProperties(propertyCount, null);
        VkLayerProperties.Buffer properties =
                VkLayerProperties.mallocStack(propertyCount.get(0), stack);
        vkEnumerateInstanceLayerProperties(propertyCount, properties);
        return properties;
    }

    /** Creates default debug messenger info for logging. */
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

    /** VK logging entrypoint. */
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

        log.log(level, callbackData.pMessageString());

        return VK_FALSE;
    }

    /** Initializes debugMessenger to receive VK log messages. */
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

    /// Setup window surface.

    /**
     * Create a window surface.
     *
     * <p>This method uses window to get its surface that the renderer will draw to
     */
    private long createSurface() {
        log.fine("Create surface");

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

    /** Sets up one physical device for use. */
    private PhysicalDevice pickPhysicalDevice() {
        log.fine("Pick physical device");
        PhysicalDevice physicalDevice =
                PhysicalDevice.pickPhysicalDevice(
                        mInstance, mSurface, TARGET_GPU, DEVICE_EXTENSIONS);
        if (physicalDevice == null) {
            throw new RuntimeException("Failed to find compatible GPU!");
        }
        log.fine(String.format("Picked GPU: %s", physicalDevice.getDeviceName()));
        return physicalDevice;
    }

    /// Logical device setup

    /** Creates a logical device with required features. */
    private VkDevice createLogicalDevice() {
        log.fine("Create logical device");

        try (MemoryStack stack = stackPush()) {
            FloatBuffer queuePriority = stack.floats(1.0f);

            int[] families = mPhysicalDevice.getIndices().uniqueFamilies();

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
            deviceFeatures.samplerAnisotropy(mPhysicalDevice.getFeatureSupport().mAnisotropyEnable);
            deviceFeatures.geometryShader(true);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.callocStack(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfo);
            createInfo.pEnabledFeatures(deviceFeatures);
            createInfo.ppEnabledExtensionNames(toPointerBuffer(DEVICE_EXTENSIONS, stack));

            if (DEBUG_MODE) {
                createInfo.ppEnabledLayerNames(
                        toPointerBuffer(WANTED_VALIDATION_LAYERS_LIST, stack));
            }

            PointerBuffer pDevice = stack.callocPointer(1);

            int result = vkCreateDevice(mPhysicalDevice.getDevice(), createInfo, null, pDevice);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create VK logical device! Err: %x", -result));
            }

            VkDevice device = new VkDevice(pDevice.get(0), mPhysicalDevice.getDevice(), createInfo);

            return device;
        }
    }

    /// Queue setup

    /**
     * Create a graphics queue.
     *
     * <p>This queue is used to submit graphics commands every frame
     */
    private VkQueue createGraphicsQueue() {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pQueue = stack.callocPointer(1);
            vkGetDeviceQueue(mDevice, mPhysicalDevice.getIndices().mGraphicsFamily, 0, pQueue);
            return new VkQueue(pQueue.get(0), mDevice);
        }
    }

    /**
     * Create a presentation queue.
     *
     * <p>This queue is used to display rendered frames on the screen
     */
    private VkQueue createPresentQueue() {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pQueue = stack.callocPointer(1);
            vkGetDeviceQueue(mDevice, mPhysicalDevice.getIndices().mPresentFamily, 0, pQueue);
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
        log.fine("Create command pool");

        try (MemoryStack stack = stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.callocStack(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(mPhysicalDevice.getIndices().mGraphicsFamily);
            poolInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

            LongBuffer pCommandPool = stack.longs(0);

            int result = vkCreateCommandPool(mDevice, poolInfo, null, pCommandPool);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create command pool! Err: %x", -result));
            }

            return pCommandPool.get(0);
        }
    }

    /// Swapchain setup

    /** Sets up the swapchain required for rendering. */
    private long createSwapchain() {
        log.fine("Setup swapchain");

        try (MemoryStack stack = stackPush()) {
            int presentMode = mPhysicalDevice.getSwapchainSupport().choosePresentMode();
            int imageCount = mPhysicalDevice.getSwapchainSupport().chooseImageCount();

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
                                mPhysicalDevice.getIndices().mGraphicsFamily,
                                mPhysicalDevice.getIndices().mPresentFamily);
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                createInfo.pQueueFamilyIndices(indices);
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            createInfo.preTransform(
                    mPhysicalDevice.getSwapchainSupport().mCapabilities.currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);

            LongBuffer pSwapchain = stack.longs(0);

            int result = vkCreateSwapchainKHR(mDevice, createInfo, null, pSwapchain);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create swapchain! Error: %x", -result));
            }

            return pSwapchain.get(0);
        }
    }

    /// Per swapchain image setup

    /** Get the number of images swapchain was created with. */
    private int getImageCount() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pImageCount = stack.ints(0);

            vkGetSwapchainImagesKHR(mDevice, mSwapchain, pImageCount, null);

            return pImageCount.get(0);
        }
    }

    /** Create a context for each swapchain image. */
    private ImageContext[] createImageContexts(int imageCount) {
        try (MemoryStack stack = stackPush()) {
            // Get swapchain images
            LongBuffer pSwapchainImages = stack.mallocLong(imageCount);

            IntBuffer pImageCount = stack.ints(imageCount);

            vkGetSwapchainImagesKHR(mDevice, mSwapchain, pImageCount, pSwapchainImages);

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
                                            new VulkanImage(
                                                    mDevice,
                                                    mSurfaceFormat.format(),
                                                    pSwapchainImages.get(i)),
                                            i,
                                            buffers.get(i)))
                    .toArray(ImageContext[]::new);
        }
    }

    /// Render pass setup

    /**
     * Create a render pass.
     *
     * <p>This method will describe how a single render pass should behave.
     *
     * <p>TODO: move to material system? Move to render pass manager?
     */
    private long createRenderPass() {
        log.fine("Create render pass");

        try (MemoryStack stack = stackPush()) {
            VkAttachmentDescription.Buffer attachments =
                    VkAttachmentDescription.callocStack(3, stack);
            VkAttachmentDescription colorAttachment = attachments.get(0);
            colorAttachment.format(mSurfaceFormat.format());
            colorAttachment.samples(mMSAASamples);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            // We don't use stencils yet
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            // We present the image after rendering, and don't care what it was,
            // since we clear it anyways.
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkAttachmentReference.Buffer colorAttachmentRef =
                    VkAttachmentReference.callocStack(1, stack);
            colorAttachmentRef.attachment(0);
            colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkAttachmentDescription depthAttachment = attachments.get(1);
            depthAttachment.format(mPhysicalDevice.findDepthFormat());
            depthAttachment.samples(mMSAASamples);
            depthAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            depthAttachment.storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            depthAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            depthAttachment.stencilLoadOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            depthAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            depthAttachment.finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkAttachmentReference depthAttachmentRef = VkAttachmentReference.callocStack(stack);
            depthAttachmentRef.attachment(1);
            depthAttachmentRef.layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkAttachmentDescription resolveAttachment = attachments.get(2);
            resolveAttachment.format(mSurfaceFormat.format());
            resolveAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
            resolveAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            resolveAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            resolveAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            resolveAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            resolveAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            resolveAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            VkAttachmentReference.Buffer resolveAttachmentRef =
                    VkAttachmentReference.callocStack(1, stack);
            resolveAttachmentRef.attachment(2);
            resolveAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.callocStack(1, stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(colorAttachmentRef);
            subpass.pDepthStencilAttachment(depthAttachmentRef);
            subpass.pResolveAttachments(resolveAttachmentRef);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.callocStack(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(attachments);
            renderPassInfo.pSubpasses(subpass);

            // Make render passes wait for COLOR_ATTACHMENT_OUTPUT stage
            VkSubpassDependency.Buffer dependency = VkSubpassDependency.callocStack(1, stack);
            dependency.srcSubpass(VK_SUBPASS_EXTERNAL);
            dependency.dstSubpass(0);
            dependency.srcStageMask(
                    VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                            | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT);
            dependency.srcAccessMask(0);
            dependency.dstStageMask(
                    VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                            | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT);
            dependency.dstAccessMask(
                    VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
                            | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);

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

    /// Color texture setup

    private VulkanImage createColorImage() {
        VkCommandBuffer tmpCommandBuffer = beginSingleUseCommandBuffer();
        VulkanImage colorImage =
                new VulkanImage(
                        tmpCommandBuffer,
                        mDevice,
                        mPhysicalDevice,
                        mExtent.width(),
                        mExtent.height(),
                        VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
                                | VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT,
                        mMSAASamples,
                        mSurfaceFormat.format(),
                        VK_IMAGE_ASPECT_COLOR_BIT,
                        VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        endSingleUseCommandBuffer(tmpCommandBuffer);
        return colorImage;
    }

    /// Depth texture setup

    private VulkanImage createDepthImage() {
        VkCommandBuffer tmpCommandBuffer = beginSingleUseCommandBuffer();
        VulkanImage depthImage =
                VulkanImage.createDepthImage(
                        tmpCommandBuffer,
                        mDevice,
                        mPhysicalDevice,
                        mExtent.width(),
                        mExtent.height(),
                        mMSAASamples);
        endSingleUseCommandBuffer(tmpCommandBuffer);
        return depthImage;
    }

    /// Vertex and index buffers

    /**
     * Create a instance buffer.
     *
     * <p>As the name implies, this buffer holds base per-instance data
     */
    private VulkanBuffer createInstanceBuffer(int sizeOfBuffer) {
        log.fine("Create instance buffer");

        try (MemoryStack stack = stackPush()) {
            return new VulkanBuffer(
                    mDevice,
                    mPhysicalDevice,
                    sizeOfBuffer,
                    // VK_BUFFER_USAGE_TRANSFER_DST_BIVK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |T
                    VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        }
    }

    /**
     * Create a single use command buffer.
     *
     * <p>This command buffer can be flushed with {@code endSingleUseCommandBuffer}
     */
    static VkCommandBuffer beginSingleUseCommandBuffer(VkDevice device, long commandPool) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandPool(commandPool);
            allocInfo.commandBufferCount(1);

            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer);

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommandBuffer.get(0), device);

            vkBeginCommandBuffer(commandBuffer, beginInfo);

            return commandBuffer;
        }
    }

    private VkCommandBuffer beginSingleUseCommandBuffer() {
        return beginSingleUseCommandBuffer(mDevice, mCommandPool);
    }

    /** Ends and frees the single use command buffer. */
    static void endSingleUseCommandBuffer(
            VkCommandBuffer commandBuffer,
            VkDevice device,
            VkQueue graphicsQueue,
            long commandPool) {
        try (MemoryStack stack = stackPush()) {
            vkEndCommandBuffer(commandBuffer);

            PointerBuffer pCommandBuffer = stack.pointers(commandBuffer.address());

            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(pCommandBuffer);

            vkQueueSubmit(graphicsQueue, submitInfo, NULL);
            vkQueueWaitIdle(graphicsQueue);

            vkFreeCommandBuffers(device, commandPool, pCommandBuffer);
        }
    }

    private void endSingleUseCommandBuffer(VkCommandBuffer commandBuffer) {
        endSingleUseCommandBuffer(commandBuffer, mDevice, mGraphicsQueue, mCommandPool);
    }

    /// Frame Context setup

    private FrameContext[] createFrameContexts(int framesInFlight) {
        log.fine("Setup sync objects");

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

                                ctx.mImageAvailableSemaphore = pSem1.get(0);
                                ctx.mRenderFinishedSemaphore = pSem2.get(0);
                                ctx.mInFlightFence = pFence.get(0);

                                frames[i] = ctx;
                            });
            return frames;
        }
    }

    /// Record command buffer to temporary object

    private TreeMap<Integer, List<DrawCallState>> mToPresort = new TreeMap<>();
    private TreeMap<Integer, TreeMap<Float, List<NonInstancedDraw>>> mPreSorted = new TreeMap<>();

    void updateInstanceBuffer(ImageContext ctx, List<Renderable> renderables, List<Light> lights) {

        mToPresort.clear();

        mCurrentMeshBuffer.cleanupUnusedMeshes();

        for (Map<DrawCallState.HashKey, DrawCallState> stateMap : mDrawInstances.values()) {
            for (DrawCallState state : stateMap.values()) {
                state.startDrawData();
            }
        }

        DrawCallState.HashKey tmpKey = new DrawCallState.HashKey();

        for (Renderable renderable : renderables) {
            if (renderable.getMesh() == null) {
                continue;
            }

            tmpKey.setRenderable(renderable);

            ShaderSet shaderSet = renderable.getMaterial().getShaderSet();
            Integer renderOrder = shaderSet.mRenderOrder;

            Map<DrawCallState.HashKey, DrawCallState> stateMap = mDrawInstances.get(renderOrder);

            if (stateMap == null) {
                stateMap = new HashMap<>();
                mDrawInstances.put(renderOrder, stateMap);
            }

            DrawCallState state = stateMap.get(tmpKey);
            if (state == null) {
                // We don't want to put the temp key in, becuase it changes values
                DrawCallState.HashKey newKey = new DrawCallState.HashKey(renderable);
                state = new DrawCallState(this, mImageContexts.length, newKey);
                state.startDrawData();
                stateMap.put(newKey, state);
            }
            state.addObject(renderable);
        }

        mDrawInstances
                .entrySet()
                .removeIf(
                        e -> {
                            e.getValue().entrySet().removeIf(e2 -> e2.getValue().shouldCleanup());
                            return e.getValue().isEmpty();
                        });

        int instanceBufferSize = 0;

        for (Map<DrawCallState.HashKey, DrawCallState> stateMap : mDrawInstances.values()) {
            for (DrawCallState state : stateMap.values()) {
                state.updateMeshBuffer(mCurrentMeshBuffer);
                instanceBufferSize = state.setInstanceBufferOffset(instanceBufferSize);

                ShaderSet shaderSet = state.getShaderSet();

                if (shaderSet.isPreSort()) {
                    Integer renderOrder = shaderSet.mRenderOrder;

                    List<DrawCallState> sortState = mToPresort.get(renderOrder);
                    if (sortState == null) {
                        sortState = new ArrayList<>();
                        mToPresort.put(renderOrder, sortState);
                    }
                    sortState.add(state);
                }
            }
        }

        if (instanceBufferSize > ctx.mInstanceBufferSize) {
            int cursize = mInstanceBufferSize > 0 ? mInstanceBufferSize : 4096;
            while (instanceBufferSize > cursize) {
                cursize *= 2;
            }
            if (ctx.mInstanceBuffer != null) {
                ctx.mInstanceBuffer.free();
            }
            ctx.mInstanceBuffer = createInstanceBuffer(cursize);
            ctx.mInstanceBufferSize = cursize;
            mInstanceBufferSize = cursize;
        }

        if (mCurrentMeshBuffer.isDirty()) {
            mDiscardedMeshBuffers.put(ctx.mImageIndex, mCurrentMeshBuffer);
            mCurrentMeshBuffer = mCurrentMeshBuffer.commitChanges(mGraphicsQueue, mCommandPool);
        }

        try (MemoryStack stack = stackPush()) {
            PointerBuffer pData = stack.pointers(0);
            int res =
                    vkMapMemory(
                            mDevice, ctx.mInstanceBuffer.mMemory, 0, instanceBufferSize, 0, pData);

            if (res == VK_SUCCESS) {
                ByteBuffer byteBuffer = pData.getByteBuffer(instanceBufferSize);

                for (Map<DrawCallState.HashKey, DrawCallState> stateMap : mDrawInstances.values()) {
                    for (DrawCallState state : stateMap.values()) {
                        state.updateInstanceBuffer(byteBuffer, lights);
                        state.endDrawData(ctx.mImageIndex);
                    }
                }

                vkUnmapMemory(mDevice, ctx.mInstanceBuffer.mMemory);
            } else {
                for (Map<DrawCallState.HashKey, DrawCallState> stateMap : mDrawInstances.values()) {
                    for (DrawCallState state : stateMap.values()) {
                        state.slowUpdateInstanceBuffer(pData, ctx.mInstanceBuffer.mMemory, lights);
                        state.endDrawData(ctx.mImageIndex);
                    }
                }
            }
        }
    }

    void recordCommandBuffer(ImageContext ctx, Camera camera) {

        mInstancedCalls = 0;
        mSlowCalls = 0;

        mVertexConstants.mProj = camera.getProj();
        mVertexConstants.mView = camera.getView();

        mPreSorted.clear();
        Vector3f camPosition = camera.getGameObject().getTransform().getPosition();
        Vector3f tmpVec = new Vector3f();

        // Presort objects that need to be rendered in a sorted manner
        for (Integer key : mToPresort.keySet()) {
            List<DrawCallState> states = mToPresort.get(key);
            TreeMap<Float, List<NonInstancedDraw>> output = new TreeMap<>();
            for (DrawCallState state : states) {
                for (DrawData drawData : state.getDrawData()) {
                    int objID = 0;
                    for (Renderable obj : drawData.mObjects) {
                        Float dist = obj.getDepth(camPosition, tmpVec);
                        List<NonInstancedDraw> equalList = output.get(dist);
                        if (equalList == null) {
                            equalList = new ArrayList<>();
                            output.put(dist, equalList);
                        }
                        equalList.add(new NonInstancedDraw(state, drawData, objID++));
                    }
                }
            }
            mPreSorted.put(key, output);
        }

        try (MemoryStack stack = stackPush()) {
            // Record the command buffers
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.callocStack(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
            renderPassInfo.renderPass(mRenderPass);

            renderPassInfo.renderArea().offset().clear();
            renderPassInfo.renderArea().extent(mExtent);

            VkClearValue.Buffer clearColor = VkClearValue.callocStack(2, stack);

            VkClearDepthStencilValue depthValue = clearColor.get(1).depthStencil();
            depthValue.depth(1.0f);
            depthValue.stencil(0);

            renderPassInfo.pClearValues(clearColor);

            int res = vkBeginCommandBuffer(ctx.mCommandBuffer, beginInfo);

            if (res != VK_SUCCESS) {
                String format =
                        String.format("Failed to begin recording command buffer! Err: %x", res);
                throw new RuntimeException(format);
            }

            renderPassInfo.framebuffer(ctx.mFramebuffer);

            // This is the beginning :)
            vkCmdBeginRenderPass(ctx.mCommandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

            ByteBuffer pConstants = stack.calloc(VertexConstants.SIZEOF);
            mVertexConstants.copyTo(pConstants);

            LongBuffer vertexBuffers =
                    stack.longs(mCurrentMeshBuffer.getVertexBuffer(), ctx.mInstanceBuffer.mBuffer);

            // Render regular objects in an instanced manner
            for (Map<DrawCallState.HashKey, DrawCallState> stateMap : mDrawInstances.values()) {
                for (DrawCallState callState : stateMap.values()) {

                    Collection<DrawData> drawDataCollection = callState.getDrawData();

                    if (drawDataCollection.isEmpty() || callState.getShaderSet().isPreSort()) {
                        continue;
                    }

                    VulkanPipeline pipeline = callState.getPipeline();

                    vkCmdBindPipeline(
                            ctx.mCommandBuffer,
                            VK_PIPELINE_BIND_POINT_GRAPHICS,
                            pipeline.mPipeline);

                    vkCmdPushConstants(
                            ctx.mCommandBuffer,
                            pipeline.mLayout,
                            VK_SHADER_STAGE_VERTEX_BIT,
                            0,
                            pConstants);

                    for (DrawCallState.DrawData drawData : drawDataCollection) {
                        try (MemoryStack innerStack = stackPush()) {
                            VulkanMeshBuffer.MeshDescriptor meshDescriptor =
                                    drawData.getMeshDescriptor();

                            LongBuffer offsets =
                                    innerStack.longs(
                                            meshDescriptor.getVertexOffset(),
                                            drawData.getInstanceBufferOffset());
                            vkCmdBindVertexBuffers(ctx.mCommandBuffer, 0, vertexBuffers, offsets);
                            vkCmdBindIndexBuffer(
                                    ctx.mCommandBuffer,
                                    mCurrentMeshBuffer.getIndexBuffer(),
                                    meshDescriptor.getIndexOffset(),
                                    VK_INDEX_TYPE_UINT32);

                            long[] descriptorSets = drawData.getDescriptorSets();

                            if (descriptorSets != null && descriptorSets.length > 0) {
                                LongBuffer pDescriptorSets = innerStack.longs(descriptorSets);

                                vkCmdBindDescriptorSets(
                                        ctx.mCommandBuffer,
                                        VK_PIPELINE_BIND_POINT_GRAPHICS,
                                        pipeline.mLayout,
                                        0,
                                        pDescriptorSets,
                                        null);
                            }

                            mInstancedCalls++;
                            vkCmdDrawIndexed(
                                    ctx.mCommandBuffer,
                                    meshDescriptor.getIndexCount(),
                                    drawData.getObjects().size(),
                                    0,
                                    0,
                                    0);
                        }
                    }
                }
            }

            // Render sorted objects one by one. Sadly, we can not batch them.
            for (TreeMap<Float, List<NonInstancedDraw>> renderOrderGroup : mPreSorted.values()) {
                for (List<NonInstancedDraw> objects : renderOrderGroup.descendingMap().values()) {
                    for (NonInstancedDraw object : objects) {
                        try (MemoryStack innerStack = stackPush()) {

                            VulkanPipeline pipeline = object.getState().getPipeline();
                            VulkanMeshBuffer.MeshDescriptor meshDescriptor =
                                    object.getData().getMeshDescriptor();

                            vkCmdBindPipeline(
                                    ctx.mCommandBuffer,
                                    VK_PIPELINE_BIND_POINT_GRAPHICS,
                                    pipeline.mPipeline);

                            vkCmdPushConstants(
                                    ctx.mCommandBuffer,
                                    pipeline.mLayout,
                                    VK_SHADER_STAGE_VERTEX_BIT,
                                    0,
                                    pConstants);

                            LongBuffer offsets =
                                    innerStack.longs(
                                            meshDescriptor.getVertexOffset(),
                                            object.getInstanceBufferOffset());
                            vkCmdBindVertexBuffers(ctx.mCommandBuffer, 0, vertexBuffers, offsets);
                            vkCmdBindIndexBuffer(
                                    ctx.mCommandBuffer,
                                    mCurrentMeshBuffer.getIndexBuffer(),
                                    meshDescriptor.getIndexOffset(),
                                    VK_INDEX_TYPE_UINT32);

                            long[] descriptorSets = object.getData().getDescriptorSets();

                            if (descriptorSets != null && descriptorSets.length > 0) {
                                LongBuffer pDescriptorSets = innerStack.longs(descriptorSets);

                                vkCmdBindDescriptorSets(
                                        ctx.mCommandBuffer,
                                        VK_PIPELINE_BIND_POINT_GRAPHICS,
                                        pipeline.mLayout,
                                        0,
                                        pDescriptorSets,
                                        null);
                            }

                            mSlowCalls++;
                            vkCmdDrawIndexed(
                                    ctx.mCommandBuffer, meshDescriptor.getIndexCount(), 1, 0, 0, 0);
                        }
                    }
                }
            }

            vkCmdEndRenderPass(ctx.mCommandBuffer);

            // And this is the end
            res = vkEndCommandBuffer(ctx.mCommandBuffer);

            if (res != VK_SUCCESS) {
                String format =
                        String.format("Failed to end recording command buffer! Err: %x", res);
                throw new RuntimeException(format);
            }
        }
    }

    /// Setup texture

    /// Cleanup code

    /** Destroys debugMessenger if exists. */
    private void destroyDebugMessanger() {
        if (mDebugMessenger == 0) {
            return;
        }
        vkDestroyDebugUtilsMessengerEXT(mInstance, mDebugMessenger, null);
    }
}
