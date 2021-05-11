/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static java.util.stream.Collectors.toSet;
import static org.dragonskulle.utils.Env.envBool;
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
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
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
 *     call {@link Renderer#onResize} when its window gets resized.
 *     <p>Use {@link Renderer#render} method to render a frame.
 *     <p>This renderer was originally based on<a href="https://vulkan-tutorial.com/">Vulkan
 *     Tutorial</a>, and was later rewritten with a much more manageable design.
 */
@Accessors(prefix = "m")
@Getter(AccessLevel.PACKAGE)
@Log
public class Renderer implements NativeResource {

    /** Reference to the GLFW window. */
    private long mWindow;

    /** A vulkan context instance. */
    private VkInstance mInstance;
    /** Debug messenger used to log validation errors when {@code DEBUG_RENDERER} is enabled. */
    private long mDebugMessenger;
    /** Surface of the window. */
    private long mSurface;

    /** Active physical device. */
    private PhysicalDevice mPhysicalDevice;

    /** Logical device. */
    private VkDevice mDevice;
    /** Queue that's used to submit graphics commands to. */
    private VkQueue mGraphicsQueue;
    /** Queue that's used to present things on screen. */
    private VkQueue mPresentQueue;

    /** Pool for command buffers. */
    private long mCommandPool;

    /** Factory for on-GPU texture samplers. */
    private TextureSamplerFactory mSamplerFactory;
    /** Factory for on-GPU sampled textures. */
    private VulkanSampledTextureFactory mTextureFactory;
    /** Factory for descriptor set layouts when using a number of textures. */
    private TextureSetLayoutFactory mTextureSetLayoutFactory;
    /** Factory for individual image descriptor sets. */
    private TextureSetFactory mTextureSetFactory;
    /**
     * Constants that are passed to vertex shaders (camera view and perspective projection
     * matrices).
     */
    private VertexConstants mVertexConstants = new VertexConstants();

    /** Active surface format. */
    private VkSurfaceFormatKHR mSurfaceFormat;
    /** Dimensions of the window. */
    private VkExtent2D mExtent;
    /** Handle to swapchain. */
    private long mSwapchain;
    /** Handle to the default render pass. */
    private long mRenderPass;

    /**
     * An array of image contexts. This is used so that we keep the GPU fed with commands, and tell
     * it to draw the next frame before the previous ones have finished rendering
     */
    private ImageContext[] mImageContexts;
    /**
     * An array of frame contexts. This has size FRAMES_IN_FLIGHT which controls how many frames we
     * are supposed to have in flight.
     */
    private FrameContext[] mFrameContexts;

    /** Size of the instance buffer in bytes. */
    @Getter(AccessLevel.PUBLIC)
    private int mInstanceBufferSize = 0;

    /** Handle to colour attachment (multisampled off-screen buffer). */
    private VulkanImage mColorImage;
    /** View to {@code mColorImage}. */
    private long mColorImageView;

    /** Handle to depth attachment (off-screen buffer). */
    private VulkanImage mDepthImage;
    /** View to {@code mDepthImage}. */
    private long mDepthImageView;

    /** Number of frames elapsed. */
    private int mFrameCounter = 0;

    /** Tells how many instanced draw calls happened last frame. */
    @Getter(AccessLevel.PUBLIC)
    private int mInstancedCalls = 0;

    /** Tells how many non-instanced draw calls happened last frame. */
    @Getter(AccessLevel.PUBLIC)
    private int mSlowCalls = 0;

    /** The current mesh buffer that contains all meshes on the GPU. */
    private VulkanMeshBuffer mCurrentMeshBuffer;
    /**
     * Maps image index to any mesh buffers that were discarded on that frame. This is so we free
     * the mesh buffer the next time the same image is used, and we can guarantee that all frames
     * that used the buffer have rendered.
     *
     * <p>It would be possible to optimize the release of resources in cases where mesh buffer is
     * updated every frame, but that would be a future improvement.
     */
    private Map<Integer, VulkanMeshBuffer> mDiscardedMeshBuffers = new HashMap<>();

    /**
     * Maps image index to any draw states that were discarded on that frame. This is so we free the
     * state the next time the same image is used, and we can guarantee that all frames that used
     * the state have finished rendering.
     */
    private Map<Integer, List<DrawCallState>> mDiscardedDrawCallStates = new HashMap<>();

    /**
     * Maps render orders to maps of draw call states. This allows us to change the order draws are
     * done in and keep similar draws packed together in instantiatable draw calls.
     *
     * <p>TreeMap is used to iterate through the maps in correct order.
     *
     * <p>A future expansion here would be to add another layer of indirection for individual render
     * passes.
     */
    private TreeMap<Integer, Map<ShaderSet, DrawCallState>> mDrawInstances = new TreeMap<>();

    /**
     * Maps a render order to a list of unsorted objects that are not supposed to be drawn in
     * instanced fashion.
     */
    private TreeMap<Integer, List<DrawCallState>> mToPresort = new TreeMap<>();
    /** Maps a render order to list of non-instanced draw calls. */
    private TreeMap<Integer, TreeMap<Float, List<NonInstancedDraw>>> mPreSorted = new TreeMap<>();

    @Getter private RendererSettings mRendererSettings;

    /** List of validation layers to activate when debug mode is on. */
    private static final List<String> WANTED_VALIDATION_LAYERS_LIST =
            Arrays.asList("VK_LAYER_KHRONOS_validation");
    /** List of device extensions that are required for the renderer to work. */
    private static final Set<String> DEVICE_EXTENSIONS =
            Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME).collect(toSet());

    /**
     * Controls whether debug mode is on or off. Set an environment variable {@code DEBUG_RENDERER}
     * to enable debug mode. This mode enables Vulkan validation layers which are extremely useful
     * in tracking down renderer bugs, if there are any. But, it requires the layers to be installed
     * (Khronos Vulkan SDK if on windows)
     */
    static final boolean DEBUG_MODE = envBool("DEBUG_RENDERER", false);
    /**
     * Target GPU to use. When {@code TARGET_GPU} environment variale is set, the renderer will only
     * pick GPUs that contain the substring of the provided value in their name. If no such GPU
     * exists, the renderer will refuse to initialise.
     */
    private static final String TARGET_GPU = envString("TARGET_GPU", null);

    /** Highest value of UINT64, it's 0xffffffffffffffff. */
    private static final long UINT64_MAX = -1L;
    /**
     * Maximum number of frames that we can render at the same time. In practice, the minimum of
     * {@code FRAMES_IN_FLIGHT}, and GPU image count will be the actual number of frames that get
     * drawn at the same time.
     */
    private static final int FRAMES_IN_FLIGHT = 4;

    /** Synchronization objects for when multiple frames are rendered at a time. */
    private class FrameContext {
        /** Semaphore for when the image is ready to be redrawn. */
        public long mImageAvailableSemaphore;
        /** Semaphore for when the image has finished rendering and is able to be displayed. */
        public long mRenderFinishedSemaphore;
        /** Fence that is used to synchronize the frame. */
        public long mInFlightFence;
    }

    /** All state for a single frame. */
    private static class ImageContext {
        /** Command buffer of the image. */
        public VkCommandBuffer mCommandBuffer;
        /**
         * Reference to a FrameContext fence, if there is a frame that's already rendering on the
         * image.
         */
        public long mInFlightFence;
        /** Handle to the framebuffer. */
        public long mFramebuffer;
        /** The index of this image context. */
        public int mImageIndex;

        /** The current size of the instance buffer for this image. */
        public int mInstanceBufferSize;
        /** The actual instance buffer (a buffer for per-instance shader data). */
        public VulkanBuffer mInstanceBuffer;

        /** A view to this image. */
        private long mImageView;
        /** Reference to {@link Renderer#mDevice}. */
        private VkDevice mDevice;

        /**
         * Create a image context.
         *
         * @param renderer handle to the parent renderer. It is needed to store reference to the
         *     underlying device, and creating a framebuffer
         * @param image the swapchain image that things will be drawn to
         * @param imageIndex the index of this image
         * @param commandBuffer the command buffer that will be used to record rendering commands to
         */
        private ImageContext(
                Renderer renderer, VulkanImage image, int imageIndex, long commandBuffer) {
            this.mCommandBuffer = new VkCommandBuffer(commandBuffer, renderer.mDevice);
            this.mImageView = image.createImageView();
            this.mFramebuffer = createFramebuffer(renderer);
            this.mImageIndex = imageIndex;

            this.mDevice = renderer.mDevice;

            mInstanceBufferSize = 0;
        }

        /**
         * Create a framebuffer from image view.
         *
         * @param renderer the parent renderer.
         * @return handle to the framebuffer.
         */
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

        /** Free the underlying resources. */
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
     * {@link Renderer#render} method can be called.
     *
     * @param appName name of the rendered application.
     * @param window handle to GLFW window.
     * @throws RuntimeException when initialization fails.
     */
    public Renderer(String appName, long window, RendererSettings settings)
            throws RuntimeException {
        log.fine("Initialize renderer");
        mRendererSettings = settings;
        mInstanceBufferSize = 4096;
        this.mWindow = window;
        mInstance = createInstance(appName);
        if (DEBUG_MODE) {
            mDebugMessenger = createDebugLogger();
        }
        mSurface = createSurface();
        mPhysicalDevice = pickPhysicalDevice();
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

    public int getSupportedMSAASamples() {
        return mPhysicalDevice.getFeatureSupport().getMsaaSamples();
    }

    public String getPhysicalDeviceName() {
        return mPhysicalDevice.getDeviceName();
    }

    public VBlankMode[] getVBlankModes() {
        return mPhysicalDevice.getSwapchainSupport().mVBlankModes;
    }

    /**
     * Set renderer graphics settings.
     *
     * <p>Depending on the settings changed, there may be a longer, or shorter time rendering
     * freeze.
     *
     * @param newSettings new graphics settings to choose
     */
    public void setSettings(RendererSettings newSettings) {
        RendererSettings oldSettings = mRendererSettings;

        if (newSettings.equals(oldSettings)) {
            return;
        }

        mRendererSettings = newSettings;
        recreateSwapchain();
    }

    /**
     * Render a frame.
     *
     * <p>This method will take a list of renderable objects, alongside lights, and render them from
     * the camera point of view.
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

            List<DrawCallState> discardedStates = mDiscardedDrawCallStates.get(imageIndex);

            if (discardedStates != null) {
                for (DrawCallState state : discardedStates) {
                    state.free();
                }
                discardedStates.clear();
            }

            Matrix4f combined = new Matrix4f();

            combined.set(camera.getProj());
            combined.mul(camera.getView());

            updateInstanceBuffer(image, new FrustumIntersection(combined), objects, lights);
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

        for (Map<ShaderSet, DrawCallState> stateMap : mDrawInstances.values()) {
            for (DrawCallState state : stateMap.values()) {
                state.free();
            }
        }
        mDrawInstances.clear();

        for (VulkanMeshBuffer meshBuffer : mDiscardedMeshBuffers.values()) {
            meshBuffer.free();
        }
        mDiscardedMeshBuffers.clear();

        for (List<DrawCallState> discardedStates : mDiscardedDrawCallStates.values()) {
            if (discardedStates != null) {
                for (DrawCallState state : discardedStates) {
                    state.free();
                }
                discardedStates.clear();
            }
        }

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
        mRendererSettings.setMSAACount(
                mPhysicalDevice.findSuitableMSAACount(mRendererSettings.getMSAACount()));
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
     *
     * @return the created VkInstance
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
            createInfo.ppEnabledExtensionNames(getExtensions(stack));

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

    /**
     * Returns required extensions for the VK context.
     *
     * @param stack the stack that will be used to put the extensions to (if debug mode is on,
     *     otherwise just glfwExtensions will be returned)
     * @return pointer buffer with the names of the extensions that are required for the Vulkan
     *     context.
     */
    private PointerBuffer getExtensions(MemoryStack stack) {
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
     * Sets up validation layers used for debugging
     *
     * <p>Throws if the layers were not available, createInfo gets bound to the data on the stack
     * frame. Do not pop the stack before using up createInfo!!!
     *
     * @param createInfo instance creation info that will have the validation layers set
     * @param stack stack of the caller for where the validation layer info will be stored on
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

    /**
     * Utility for converting collection to pointer buffer.
     *
     * @param collection collection to convert the pointer buffer to
     * @param stack stack of the caller for where the data will be stored on
     * @return the pointer buffer of strings
     */
    private PointerBuffer toPointerBuffer(Collection<String> collection, MemoryStack stack) {
        PointerBuffer buffer = stack.mallocPointer(collection.size());
        collection.stream().map(stack::UTF8).forEach(buffer::put);
        return buffer.rewind();
    }

    /**
     * Utility for converting a collection of pointer types to pointer buffer.
     *
     * @param array the array to convert
     * @param stack stack of the caller for where the data will be stored on
     * @return the pointer buffer representing the data
     */
    private <T extends Pointer> PointerBuffer toPointerBuffer(T[] array, MemoryStack stack) {
        PointerBuffer buffer = stack.mallocPointer(array.length);
        Arrays.stream(array).forEach(buffer::put);
        return buffer.rewind();
    }

    /**
     * Utility for retrieving instance VkLayerProperties list.
     *
     * @param stack stack of the caller for where the data will be stored on
     * @return properties for the instance layers
     */
    private VkLayerProperties.Buffer getInstanceLayerProperties(MemoryStack stack) {
        IntBuffer propertyCount = stack.ints(0);
        vkEnumerateInstanceLayerProperties(propertyCount, null);
        VkLayerProperties.Buffer properties =
                VkLayerProperties.mallocStack(propertyCount.get(0), stack);
        vkEnumerateInstanceLayerProperties(propertyCount, properties);
        return properties;
    }

    /**
     * Creates default debug messenger info for logging.
     *
     * @param stack stack of the caller for where the data will be stored on
     * @return create struct for the debug messenger that's used for validation layers
     */
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

    /**
     * VK logging entrypoint.
     *
     * @param messageSeverity severity of the message, which will be translated to log level
     * @param messageType type of the message, unused
     * @param pCallbackData pointer to the callback data
     * @param pUserData pointer to the user data (unused)
     * @return VK_FALSE
     */
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

    /**
     * Initializes debugMessenger to receive VK log messages.
     *
     * @return handle to the newly created debug messenger
     */
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
     *
     * @return the surface
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

    /**
     * Sets up one physical device for use.
     *
     * @return the most optimal physical device
     */
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

    /**
     * Creates a logical device with required features.
     *
     * @return the logical device
     */
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
     *
     * @return the graphics queue
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
     *
     * @return the presentation queue
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
     *
     * @return the command pool
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

    /**
     * Sets up the swapchain required for rendering.
     *
     * @return the swapchain that's used to present drawn frames on the screen
     */
    private long createSwapchain() {
        log.fine("Setup swapchain");

        try (MemoryStack stack = stackPush()) {
            int presentMode =
                    mPhysicalDevice
                            .getSwapchainSupport()
                            .choosePresentMode(mRendererSettings.getVBlankMode())
                            .getValue();
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

    /**
     * Get the number of images swapchain was created with.
     *
     * @return the image count
     */
    private int getImageCount() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pImageCount = stack.ints(0);

            vkGetSwapchainImagesKHR(mDevice, mSwapchain, pImageCount, null);

            return pImageCount.get(0);
        }
    }

    /**
     * Create a context for each swapchain image.
     *
     * @param imageCount the number of images that the swapchain has
     * @return imageCount size ImageContext array that represents a context for a single swachain
     *     image
     */
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
     *
     * @return the render pass
     */
    private long createRenderPass() {
        log.fine("Create render pass");

        try (MemoryStack stack = stackPush()) {
            VkAttachmentDescription.Buffer attachments =
                    VkAttachmentDescription.callocStack(3, stack);
            VkAttachmentDescription colorAttachment = attachments.get(0);
            colorAttachment.format(mSurfaceFormat.format());
            colorAttachment.samples(mRendererSettings.getMSAACount());
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
            depthAttachment.samples(mRendererSettings.getMSAACount());
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

    /**
     * Create a colour attachment image.
     *
     * @return the colour image
     */
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
                        mRendererSettings.getMSAACount(),
                        mSurfaceFormat.format(),
                        VK_IMAGE_ASPECT_COLOR_BIT,
                        VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        endSingleUseCommandBuffer(tmpCommandBuffer);
        return colorImage;
    }

    /// Depth texture setup

    /**
     * Create a depth image that is used for depth testing
     *
     * @return the depth image
     */
    private VulkanImage createDepthImage() {
        VkCommandBuffer tmpCommandBuffer = beginSingleUseCommandBuffer();
        VulkanImage depthImage =
                VulkanImage.createDepthImage(
                        tmpCommandBuffer,
                        mDevice,
                        mPhysicalDevice,
                        mExtent.width(),
                        mExtent.height(),
                        mRendererSettings.getMSAACount());
        endSingleUseCommandBuffer(tmpCommandBuffer);
        return depthImage;
    }

    /// Vertex and index buffers

    /**
     * Create a instance buffer.
     *
     * <p>As the name implies, this buffer holds base per-instance data
     *
     * @return the instance buffer
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
     *
     * @param device the device to create the command buffer on
     * @param commandPool the command pool that's used to create the command buffer from
     * @return the newly created command buffer
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

    /**
     * Create a single use command buffer.
     *
     * <p>this method is a wrapper around {@link Renderer#beginSingleUseCommandBuffer(VkDevice,
     * long)}
     *
     * @return the newly created command buffer
     */
    private VkCommandBuffer beginSingleUseCommandBuffer() {
        return beginSingleUseCommandBuffer(mDevice, mCommandPool);
    }

    /**
     * Ends and frees the single use command buffer.
     *
     * @param commandBuffer the command buffer to end
     * @param device the device that the command buffer is on
     * @param graphicsQueue the queue that the command buffer will be submitted to
     * @param commandPool the pool of the command buffers
     */
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

    /**
     * Ends and frees the single use command buffer.
     *
     * <p>This method is essentially a wrapper around {@link
     * Renderer#endSingleUseCommandBuffer(VkCommandBuffer, VkDevice, VkQueue, long)}
     *
     * @param commandBuffer the command buffer to end
     */
    private void endSingleUseCommandBuffer(VkCommandBuffer commandBuffer) {
        endSingleUseCommandBuffer(commandBuffer, mDevice, mGraphicsQueue, mCommandPool);
    }

    /// Frame Context setup

    /**
     * Create the frame context (synchronizatin objects).
     *
     * @param framesInFlight the number of frames that can be drawn at the same time
     * @return framesInFlight sized array of FrameContext
     */
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

    /**
     * Updates the instance buffer.
     *
     * <p>This method will update the instance buffer, load any unloaded meshes, batch up the list
     * of objects into instantiatable draw calls, and generate a list of non-instanced draws
     *
     * @param ctx the image context to update the instance buffer for
     * @param intersector frustum intersector for the objects
     * @param renderables the list of objects that need to be rendered
     * @param lights the list of lights that exist in the world
     */
    void updateInstanceBuffer(
            ImageContext ctx,
            FrustumIntersection intersector,
            List<Renderable> renderables,
            List<Light> lights) {

        mToPresort.clear();

        mCurrentMeshBuffer.cleanupUnusedMeshes();

        for (Map<ShaderSet, DrawCallState> stateMap : mDrawInstances.values()) {
            for (DrawCallState state : stateMap.values()) {
                state.startDrawData();
            }
        }

        for (Renderable renderable : renderables) {
            if (renderable.getMesh() == null) {
                continue;
            }

            Mesh m = renderable.getMesh();

            if (m.getVertices().length == 0 || m.getIndices().length == 0) {
                continue;
            }

            if (!renderable.frustumCull(intersector)) {
                continue;
            }

            ShaderSet shaderSet = renderable.getMaterial().getShaderSet();
            Integer renderOrder = shaderSet.mRenderOrder;

            Map<ShaderSet, DrawCallState> stateMap = mDrawInstances.get(renderOrder);

            if (stateMap == null) {
                stateMap = new HashMap<>();
                mDrawInstances.put(renderOrder, stateMap);
            }

            DrawCallState state = stateMap.get(shaderSet);
            if (state == null) {
                state = new DrawCallState(this, mImageContexts.length, shaderSet);
                state.startDrawData();
                stateMap.put(shaderSet, state);
            }
            state.addObject(renderable);
        }

        List<DrawCallState> discardedDrawCallStates =
                mDiscardedDrawCallStates.computeIfAbsent(ctx.mImageIndex, k -> new ArrayList<>());

        mDrawInstances
                .entrySet()
                .removeIf(
                        e -> {
                            e.getValue()
                                    .entrySet()
                                    .removeIf(
                                            e2 ->
                                                    e2.getValue()
                                                            .shouldCleanup(
                                                                    discardedDrawCallStates));
                            return e.getValue().isEmpty();
                        });

        int instanceBufferSize = 0;

        for (Map<ShaderSet, DrawCallState> stateMap : mDrawInstances.values()) {
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

        if (instanceBufferSize <= 0) {
            return;
        }

        try (MemoryStack stack = stackPush()) {
            PointerBuffer pData = stack.pointers(0);
            int res =
                    vkMapMemory(
                            mDevice, ctx.mInstanceBuffer.mMemory, 0, instanceBufferSize, 0, pData);

            if (res == VK_SUCCESS) {
                ByteBuffer byteBuffer = pData.getByteBuffer(instanceBufferSize);

                for (Map<ShaderSet, DrawCallState> stateMap : mDrawInstances.values()) {
                    for (DrawCallState state : stateMap.values()) {
                        state.updateInstanceBuffer(byteBuffer, lights);
                        state.endDrawData(ctx.mImageIndex);
                    }
                }

                vkUnmapMemory(mDevice, ctx.mInstanceBuffer.mMemory);
            } else {
                for (Map<ShaderSet, DrawCallState> stateMap : mDrawInstances.values()) {
                    for (DrawCallState state : stateMap.values()) {
                        state.slowUpdateInstanceBuffer(pData, ctx.mInstanceBuffer.mMemory, lights);
                        state.endDrawData(ctx.mImageIndex);
                    }
                }
            }
        }
    }

    /**
     * Record the command buffer for the image context
     *
     * <p>This method will record the actual draw commands that will be executed bu the GPU. Those
     * draw commands will make things go on screen.
     *
     * @param ctx the image context to record the command buffer for
     * @param camera the camera to render from
     */
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
                    mDrawInstances.size() == 0
                            ? null
                            : stack.longs(
                                    mCurrentMeshBuffer.getVertexBuffer(),
                                    ctx.mInstanceBuffer.mBuffer);

            // Render regular objects in an instanced manner
            for (Map<ShaderSet, DrawCallState> stateMap : mDrawInstances.values()) {
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
