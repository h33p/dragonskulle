/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.Configuration.DEBUG;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

import com.codepoetics.protonpack.StreamUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Getter;
import org.dragonskulle.core.Resource;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.*;

public class RenderedApp {

    @Getter private long window;

    private VkInstance instance;
    private long surface;
    private long debugMessenger;

    private PhysicalDevice physicalDevice;
    private VkDevice device;
    private VkQueue graphicsQueue;
    private VkQueue presentQueue;
    private VkSurfaceFormatKHR surfaceFormat;
    private VkExtent2D extent;
    private long swapchain;
    private long[] swapchainImages;
    private long[] swapchainImageViews;
    private long[] framebuffers;

    private long renderPass;
    private long descriptorSetLayout;
    private long pipelineLayout;
    private long graphicsPipeline;

    private long commandPool;
    private VkCommandBuffer[] commandBuffers;

    private VulkanBuffer vertexBuffer;
    private VulkanBuffer indexBuffer;

    private VulkanBuffer[] uniformBuffers;
    private UniformBufferObject[] uniformBufferObjects;

    private long descriptorPool;
    private long[] descriptorSets;

    private FrameContext[] frames;
    private long[] imagesInFlight;

    private boolean framebufferResized = false;

    public static final Logger LOGGER = Logger.getLogger("render");

    public static final boolean DEBUG_MODE;
    private static final String TARGET_GPU = envString("TARGET_GPU", null);

    private static final String[] WANTED_VALIDATION_LAYERS = {"VK_LAYER_KHRONOS_validation"};
    private static final List<String> WANTED_VALIDATION_LAYERS_LIST =
            Arrays.asList(WANTED_VALIDATION_LAYERS);

    private static Set<String> DEVICE_EXTENSIONS =
            Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME).collect(toSet());

    private static final long UINT64_MAX = -1L;

    private static final int FRAMES_IN_FLIGHT = 4;

    private static final int VBLANK_MODE = envInt("VBLANK_MODE", VK_PRESENT_MODE_MAILBOX_KHR);

    private static float RADIUS = 1.5f;

    private Resource<ShaderBuf> vertShader;
    private Resource<ShaderBuf> fragShader;

    private static Vertex[] VERTICES = {
        new Vertex(new Vector2f(0.0f, 0.0f), new Vector3f(1.0f, 1.0f, 1.0f)),
        new Vertex(new Vector2f(-0.5f * RADIUS, 0.86603f * RADIUS), new Vector3f(0.0f, 0.0f, 1.0f)),
        new Vertex(new Vector2f(0.5f * RADIUS, 0.86603f * RADIUS), new Vector3f(0.0f, 1.0f, 0.0f)),
        new Vertex(new Vector2f(RADIUS, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f)),
        new Vertex(new Vector2f(0.5f * RADIUS, -0.86603f * RADIUS), new Vector3f(0.0f, 1.0f, 0.0f)),
        new Vertex(
                new Vector2f(-0.5f * RADIUS, -0.86603f * RADIUS), new Vector3f(0.0f, 0.0f, 1.0f)),
        new Vertex(new Vector2f(-RADIUS, 0.0f), new Vector3f(0.0f, 0.0f, 1.0f)),
    };

    private static short[] INDICES = {1, 0, 2, 2, 0, 3, 3, 0, 4, 4, 0, 5, 5, 0, 6, 6, 0, 1};

    static {
        String line = System.getenv("DEBUG_RENDERER");
        DEBUG_MODE = line != null && line.equals("true");
    }

    private static class VulkanBuffer {
        private long buffer;
        private long memory;

        private void destroyBuffer(VkDevice device) {
            vkDestroyBuffer(device, buffer, null);
            vkFreeMemory(device, memory, null);
        }
    }

    private class FrameContext {
        private long imageAvailableSemaphore;
        private long renderFinishedSemaphore;
        private long inFlightFence;
    }

    private class PhysicalDevice implements Comparable<PhysicalDevice> {
        VkPhysicalDevice device;
        SwapChainSupportDetails swapchainSupport;
        String deviceName;

        int score;
        QueueFamilyIndices indices;

        PhysicalDevice(VkPhysicalDevice d) {
            device = d;
        }

        @Override
        public int compareTo(PhysicalDevice other) {
            return score == other.score ? 0 : score < other.score ? 1 : -1;
        }

        public VkExtensionProperties.Buffer getDeviceExtensionProperties(MemoryStack stack) {
            IntBuffer propertyCount = stack.ints(0);
            vkEnumerateDeviceExtensionProperties(device, (String) null, propertyCount, null);
            VkExtensionProperties.Buffer properties =
                    VkExtensionProperties.mallocStack(propertyCount.get(0), stack);
            vkEnumerateDeviceExtensionProperties(device, (String) null, propertyCount, properties);
            return properties;
        }
    }

    private class QueueFamilyIndices {
        Integer graphicsFamily;
        Integer presentFamily;

        boolean isComplete() {
            return graphicsFamily != null && presentFamily != null;
        }

        int[] uniqueFamilies() {
            return IntStream.of(graphicsFamily, presentFamily).distinct().toArray();
        }
    }

    private class SwapChainSupportDetails {
        VkSurfaceCapabilitiesKHR capabilities;
        VkSurfaceFormatKHR.Buffer formats;
        IntBuffer presentModes;

        public boolean isAdequate() {
            return formats != null && formats != null;
        }

        /** Choose a compatible surface format, prioritizing SRGB */
        public VkSurfaceFormatKHR chooseSurfaceFormat() {
            VkSurfaceFormatKHR format =
                    formats.stream()
                            .filter(
                                    f ->
                                            f.format() == VK_FORMAT_B8G8R8A8_SRGB
                                                    && f.colorSpace()
                                                            == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                            .findAny()
                            .orElseGet(() -> formats.get(0));
            LOGGER.info(
                    String.format(
                            "Picked surface format: %d %d", format.format(), format.colorSpace()));
            return format;
        }

        /** Choose a compatible presentation mode, prioritizing Triple Buffered VSync */
        public int choosePresentMode() {
            // VSync - guaranteed to be available
            int presentMode = VK_PRESENT_MODE_FIFO_KHR;

            // Prioritized tripple buffered VSync, or use no vsync
            // We need configuration for this.
            for (int i = 0; presentMode != VBLANK_MODE && i < presentModes.capacity(); i++) {
                int pm = presentModes.get(i);
                if (pm == VBLANK_MODE
                        || pm == VK_PRESENT_MODE_MAILBOX_KHR
                        || pm == VK_PRESENT_MODE_IMMEDIATE_KHR) presentMode = pm;
            }

            return presentMode;
        }

        /** Choose a compatible resolution, targetting current window resolution */
        public VkExtent2D chooseExtent(long window) {
            try (MemoryStack stack = stackPush()) {
                IntBuffer x = stack.ints(0);
                IntBuffer y = stack.ints(0);
                glfwGetFramebufferSize(window, x, y);

                LOGGER.info(String.format("Extent TRY: %dx%d", x.get(0), y.get(0)));
                LOGGER.info(
                        String.format(
                                "MAX: %dx%d",
                                capabilities.maxImageExtent().width(),
                                capabilities.maxImageExtent().height()));
                LOGGER.info(
                        String.format(
                                "MIN: %dx%d",
                                capabilities.minImageExtent().width(),
                                capabilities.minImageExtent().height()));

                VkExtent2D extent = VkExtent2D.create();
                extent.set(
                        Integer.max(
                                Integer.min(x.get(0), capabilities.maxImageExtent().width()),
                                capabilities.minImageExtent().width()),
                        Integer.max(
                                Integer.min(y.get(0), capabilities.maxImageExtent().height()),
                                capabilities.minImageExtent().height()));

                LOGGER.info(String.format("Extent: %dx%d", extent.width(), extent.height()));

                return extent;
            }
        }

        public int chooseImageCount() {
            int imageCount = capabilities.minImageCount() + 1;
            if (capabilities.maxImageCount() > 0 && imageCount > capabilities.maxImageCount())
                return capabilities.maxImageCount();
            else return imageCount;
        }
    }

    @Builder
    private static class UniformBufferObject {
        public static int SIZEOF = 4 * 4 * 4 * 3;
        public static int OFFSETOF_MODEL = 0;
        public static int OFFSETOF_VIEW = 4 * 4 * 4;
        public static int OFFSETOF_PROJ = 4 * 4 * 4 * 2;

        private Matrix4f model;
        private Matrix4f view;
        private Matrix4f proj;

        public void copyTo(ByteBuffer buffer) {
            model.get(OFFSETOF_MODEL, buffer);
            view.get(OFFSETOF_VIEW, buffer);
            proj.get(OFFSETOF_PROJ, buffer);
        }
    }

    @Builder
    private static class Vertex {
        public static int SIZEOF = (2 + 3) * 4;
        public static int OFFSETOF_POS = 0;
        public static int OFFSETOF_COL = 2 * 4;

        private Vector2fc pos;
        private Vector3fc color;

        public void copyTo(ByteBuffer buffer) {
            buffer.putFloat(pos.x());
            buffer.putFloat(pos.y());

            buffer.putFloat(color.x());
            buffer.putFloat(color.y());
            buffer.putFloat(color.z());
        }

        public static VkVertexInputBindingDescription.Buffer getBindingDescription(
                MemoryStack stack) {
            VkVertexInputBindingDescription.Buffer bindingDescription =
                    VkVertexInputBindingDescription.callocStack(1, stack);
            bindingDescription.binding(0);
            bindingDescription.stride(SIZEOF);
            bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
            return bindingDescription;
        }

        public static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions(
                MemoryStack stack) {
            VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                    VkVertexInputAttributeDescription.callocStack(2, stack);
            VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(0);
            posDescription.binding(0);
            posDescription.location(0);
            posDescription.format(VK_FORMAT_R32G32_SFLOAT);
            posDescription.offset(OFFSETOF_POS);

            VkVertexInputAttributeDescription colDescription = attributeDescriptions.get(1);
            colDescription.binding(0);
            colDescription.location(1);
            colDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
            colDescription.offset(OFFSETOF_COL);

            return attributeDescriptions;
        }
    }

    private static String envString(String key, String defaultVal) {
        String envLine = System.getenv(key);
        return envLine == null ? defaultVal : envLine;
    }

    private static int envInt(String key, int defaultVal) {
        String envLine = System.getenv(key);
        if (envLine == null) return defaultVal;
        try {
            return Integer.parseInt(envLine);
        } catch (Exception e) {
            return defaultVal;
        }
    }

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

    /// Main functions

    /** Entrypoint of the app instance */
    public void run(int width, int height, String appName) {
        DEBUG.set(DEBUG_MODE);
        initWindow(width, height, appName);
        initVulkan(appName);
        mainLoop();
        cleanup();
    }

    private void mainLoop() {
        LOGGER.info("Enter main loop");
        int frameIndex = 0;
        int frameCounter = 0;
        int seconds = 0;
        int startSecondFrame = 0;
        double lastElapsed = (double) System.currentTimeMillis() * 0.001;
        long startTime = System.currentTimeMillis();
        while (!glfwWindowShouldClose(window)) {
            try (MemoryStack stack = stackPush()) {
                long timerTime = System.currentTimeMillis();
                long elapsedMillis = timerTime - startTime;
                float curtime = (float) elapsedMillis * 0.001f;
                glfwPollEvents();
                drawFrame(stack, frames[frameIndex], curtime);
            }
            frameIndex = (frameIndex + 1) % FRAMES_IN_FLIGHT;

            // Debug FPS counter
            frameCounter++;
            long curtime = System.currentTimeMillis();
            long elapsedMillis = curtime - startTime;
            double elapsed = elapsedMillis * 0.001;
            int curSeconds = (int) (elapsedMillis / 1000);
            if (curSeconds > seconds) {
                LOGGER.info(
                        String.format(
                                "FPS: %.2f",
                                (double) (frameCounter - startSecondFrame)
                                        / (elapsed - lastElapsed)));
                seconds = curSeconds;
                startSecondFrame = frameCounter;
                lastElapsed = elapsed;
            }
        }

        vkDeviceWaitIdle(device);
    }

    private void cleanup() {
        LOGGER.info("Cleanup");
        for (FrameContext frame : frames) {
            vkDestroySemaphore(device, frame.renderFinishedSemaphore, null);
            vkDestroySemaphore(device, frame.imageAvailableSemaphore, null);
            vkDestroyFence(device, frame.inFlightFence, null);
        }
        cleanupSwapchain();
        vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null);
        fragShader.free();
        vertShader.free();
        indexBuffer.destroyBuffer(device);
        vertexBuffer.destroyBuffer(device);
        vkDestroyCommandPool(device, commandPool, null);
        vkDestroyDevice(device, null);
        destroyDebugMessanger();
        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void cleanupSwapchain() {

        for (VulkanBuffer b : uniformBuffers) b.destroyBuffer(device);

        uniformBuffers = null;
        uniformBufferObjects = null;

        vkDestroyDescriptorPool(device, descriptorPool, null);

        for (long framebuffer : framebuffers) {
            vkDestroyFramebuffer(device, framebuffer, null);
        }
        try (MemoryStack stack = stackPush()) {
            vkFreeCommandBuffers(device, commandPool, toPointerBuffer(commandBuffers, stack));
        }
        vkDestroyPipeline(device, graphicsPipeline, null);
        vkDestroyPipelineLayout(device, pipelineLayout, null);
        vkDestroyRenderPass(device, renderPass, null);
        for (long imageView : swapchainImageViews) {
            vkDestroyImageView(device, imageView, null);
        }

        vkDestroySwapchainKHR(device, swapchain, null);
    }

    private void recreateSwapchain() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer x = stack.ints(0);
            IntBuffer y = stack.ints(0);
            glfwGetFramebufferSize(window, x, y);
            LOGGER.info(String.format("%d %d", x.get(0), y.get(0)));
            while (framebufferResized || x.get(0) == 0 || y.get(0) == 0) {
                framebufferResized = false;
                glfwGetFramebufferSize(window, x, y);
                LOGGER.info(String.format("%d %d", x.get(0), y.get(0)));
                glfwWaitEvents();
            }
        }

        vkQueueWaitIdle(presentQueue);
        vkQueueWaitIdle(graphicsQueue);
        vkDeviceWaitIdle(device);

        cleanupSwapchain();

        physicalDevice.swapchainSupport = getSwapchainSupport(physicalDevice.device);

        createSwapchainObjects();
    }

    private void createSwapchainObjects() {
        setupSwapchain();
        setupImageViews();
        setupRenderPass();
        setupGraphicsPipeline();
        setupFramebuffers();
        setupUniformBuffers();
        setupDescriptorPool();
        setupDescriptorSets();
        setupCommandBuffers();
    }

    /// The main rendering

    private void drawFrame(MemoryStack stack, FrameContext ctx, float curtime) {

        if (framebufferResized) recreateSwapchain();

        vkWaitForFences(device, ctx.inFlightFence, true, UINT64_MAX);

        IntBuffer imageIndex = stack.ints(0);
        int res =
                vkAcquireNextImageKHR(
                        device,
                        swapchain,
                        UINT64_MAX,
                        ctx.imageAvailableSemaphore,
                        VK_NULL_HANDLE,
                        imageIndex);
        final int image = imageIndex.get(0);

        if (res == VK_ERROR_OUT_OF_DATE_KHR) {
            recreateSwapchain();
            return;
        }

        if (imagesInFlight[image] != 0)
            vkWaitForFences(device, imagesInFlight[image], true, UINT64_MAX);

        imagesInFlight[image] = ctx.inFlightFence;

        updateUniformBuffer(image, curtime);

        VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
        submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

        LongBuffer waitSemaphores = stack.longs(ctx.imageAvailableSemaphore);
        IntBuffer waitStages = stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
        PointerBuffer commandBuffer = stack.pointers(commandBuffers[image]);

        submitInfo.waitSemaphoreCount(1);
        submitInfo.pWaitSemaphores(waitSemaphores);
        submitInfo.pWaitDstStageMask(waitStages);
        submitInfo.pCommandBuffers(commandBuffer);

        LongBuffer signalSemaphores = stack.longs(ctx.renderFinishedSemaphore);
        submitInfo.pSignalSemaphores(signalSemaphores);

        vkResetFences(device, ctx.inFlightFence);

        res = vkQueueSubmit(graphicsQueue, submitInfo, ctx.inFlightFence);

        if (res != VK_SUCCESS) {
            vkResetFences(device, ctx.inFlightFence);
            throw new RuntimeException(
                    String.format("Failed to submit draw command buffer! Ret: %x", -res));
        }

        LongBuffer swapchains = stack.longs(swapchain);

        VkPresentInfoKHR presentInfo = VkPresentInfoKHR.callocStack(stack);
        presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
        presentInfo.pWaitSemaphores(signalSemaphores);
        presentInfo.swapchainCount(1);
        presentInfo.pSwapchains(swapchains);
        presentInfo.pImageIndices(imageIndex);

        res = vkQueuePresentKHR(presentQueue, presentInfo);

        if (res == VK_ERROR_OUT_OF_DATE_KHR || res == VK_SUBOPTIMAL_KHR) {
            recreateSwapchain();
        } else if (res != VK_SUCCESS) {
            throw new RuntimeException(String.format("Failed to present image! Ret: %x", -res));
        }
    }

    private void onFramebufferResize(long window, int width, int height) {
        framebufferResized = true;
    }

    private void updateUniformBuffer(int index, float curtime) {
        if (uniformBufferObjects[index] == null) {
            uniformBufferObjects[index] =
                    new UniformBufferObject(new Matrix4f(), new Matrix4f(), new Matrix4f());
            UniformBufferObject ubo = uniformBufferObjects[index];
            ubo.view.lookAt(
                    new Vector3f(2.0f, 2.0f, 2.0f),
                    new Vector3f(0.0f, 0.0f, -0.05f),
                    new Vector3f(0.0f, 0.0f, 1.0f));
            ubo.proj.setPerspective(
                    45.0f, (float) extent.width() / (float) extent.height(), 0.1f, 10.f, true);
            ubo.proj.m11(-ubo.proj.m11());
        }

        UniformBufferObject ubo = uniformBufferObjects[index];
        ubo.model.rotation(curtime * 1.5f, 0.0f, 0.0f, 1.0f);

        VulkanBuffer buffer = uniformBuffers[index];

        try (MemoryStack stack = stackPush()) {
            PointerBuffer pData = stack.pointers(0);
            vkMapMemory(device, buffer.memory, 0, UniformBufferObject.SIZEOF, 0, pData);
            ByteBuffer byteBuffer = pData.getByteBuffer(UniformBufferObject.SIZEOF);
            ubo.copyTo(byteBuffer);
            vkUnmapMemory(device, buffer.memory);
        }
    }

    /// Setup code

    /** Creates a GLFW window */
    private void initWindow(int width, int height, String appName) {
        LOGGER.info("Initialize GLFW window");

        if (!glfwInit()) {
            throw new RuntimeException("Cannot initialize GLFW");
        }

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(width, height, appName, NULL, NULL);

        if (window == NULL) {
            throw new RuntimeException("Cannot create window");
        }

        glfwSetFramebufferSizeCallback(window, this::onFramebufferResize);
    }

    /** initialize Vulkan context. Throw on error */
    private void initVulkan(String appName) {
        LOGGER.info("Initialize VK Context");

        setupInstance(appName);
        if (DEBUG_MODE) {
            setupDebugLogging();
        }
        setupSurface();
        setupPhysicalDevice();
        setupLogicalDevice();
        setupCommandPool();
        setupModelBuffers();
        setupShaders();
        setupDescriptorSetLayout();
        createSwapchainObjects();
        setupSyncObjects();
    }

    /// Instance setup

    private void setupInstance(String appName) {
        LOGGER.info("Setup instance");

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

            instance = new VkInstance(instancePtr.get(0), createInfo);
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
        debugCreateInfo.pfnUserCallback(RenderedApp::debugCallback);

        return debugCreateInfo;
    }

    /// Setup debug logging

    /** Initializes debugMessenger to receive VK log messages */
    private void setupDebugLogging() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pDebugMessenger = stack.longs(0);
            if (vkCreateDebugUtilsMessengerEXT(
                            instance, createDebugLoggingInfo(stack), null, pDebugMessenger)
                    != VK_SUCCESS) {
                throw new RuntimeException("Failed to initialize debug messenger");
            }
            debugMessenger = pDebugMessenger.get();
        }
    }

    /// Setup window surface

    private void setupSurface() {
        LOGGER.info("Setup surface");

        try (MemoryStack stack = stackPush()) {
            LongBuffer pSurface = stack.callocLong(1);
            int result = glfwCreateWindowSurface(instance, window, null, pSurface);
            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create windows surface! %x", -result));
            }
            surface = pSurface.get(0);
        }
    }

    /// Physical device setup

    /** Sets up one physical device for use */
    private void setupPhysicalDevice() {
        LOGGER.info("Setup physical device");
        physicalDevice = pickPhysicalDevice(TARGET_GPU);
        if (physicalDevice == null) {
            throw new RuntimeException("Failed to find compatible GPU!");
        }
        LOGGER.info(String.format("Picked GPU: %s", physicalDevice.deviceName));
    }

    /** Picks a physical device with required features */
    private PhysicalDevice pickPhysicalDevice(String targetDevice) {
        PhysicalDevice[] devices = enumeratePhysicalDevices();

        if (devices.length == 0) return null;
        else if (targetDevice == null) return devices[0];

        for (PhysicalDevice d : devices) {
            if (d.deviceName.contains(targetDevice)) {
                return d;
            }
        }

        return null;
    }

    /** Collect all compatible physical GPUs into an array, sorted by decreasing score */
    private PhysicalDevice[] enumeratePhysicalDevices() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer physDevCount = stack.ints(0);
            vkEnumeratePhysicalDevices(instance, physDevCount, null);
            PointerBuffer devices = stack.mallocPointer(physDevCount.get(0));
            vkEnumeratePhysicalDevices(instance, physDevCount, devices);

            return IntStream.range(0, devices.capacity())
                    .mapToObj(devices::get)
                    .map(d -> new VkPhysicalDevice(d, instance))
                    .map(this::collectPhysicalDeviceInfo)
                    .filter(this::isPhysicalDeviceSuitable)
                    .sorted()
                    .toArray(PhysicalDevice[]::new);
        }
    }

    /** check whether the device in question is suitable for us */
    private boolean isPhysicalDeviceSuitable(PhysicalDevice device) {
        try (MemoryStack stack = stackPush()) {
            return device.indices.isComplete()
                    && device.getDeviceExtensionProperties(stack).stream()
                            .map(VkExtensionProperties::extensionNameString)
                            .collect(toSet())
                            .containsAll(DEVICE_EXTENSIONS)
                    && device.swapchainSupport.isAdequate();
        }
    }

    /** Gathers information about physical device and stores it on `PhysicalDevice` */
    private PhysicalDevice collectPhysicalDeviceInfo(VkPhysicalDevice device) {
        PhysicalDevice physdev = new PhysicalDevice(device);

        try (MemoryStack stack = stackPush()) {
            VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.callocStack(stack);
            VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.callocStack(stack);

            vkGetPhysicalDeviceProperties(device, properties);
            vkGetPhysicalDeviceFeatures(device, features);

            physdev.deviceName = properties.deviceNameString();

            physdev.score = 0;

            // Prioritize dedicated graphics cards
            if (properties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU)
                physdev.score += 5000;

            // Check maximum texture sizes, prioritize largest
            physdev.score += properties.limits().maxImageDimension2D();

            QueueFamilyIndices indices = new QueueFamilyIndices();

            VkQueueFamilyProperties.Buffer buf = getQueueFamilyProperties(device);

            IntBuffer presentSupport = stack.ints(0);

            StreamUtils.zipWithIndex(buf.stream().map(VkQueueFamilyProperties::queueFlags))
                    .takeWhile(__ -> !indices.isComplete())
                    .forEach(
                            i -> {
                                if ((i.getValue() & VK_QUEUE_GRAPHICS_BIT) != 0)
                                    indices.graphicsFamily = Integer.valueOf((int) i.getIndex());

                                vkGetPhysicalDeviceSurfaceSupportKHR(
                                        device, (int) i.getIndex(), surface, presentSupport);

                                if (presentSupport.get(0) == VK_TRUE) {
                                    indices.presentFamily = (int) i.getIndex();
                                }
                            });

            physdev.indices = indices;
            physdev.swapchainSupport = getSwapchainSupport(device);
        }

        return physdev;
    }

    /** Utility for retrieving VkQueueFamilyProperties list */
    private VkQueueFamilyProperties.Buffer getQueueFamilyProperties(VkPhysicalDevice device) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer length = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(device, length, null);
            VkQueueFamilyProperties.Buffer props =
                    VkQueueFamilyProperties.callocStack(length.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(device, length, props);
            return props;
        }
    }

    private SwapChainSupportDetails getSwapchainSupport(VkPhysicalDevice device) {
        SwapChainSupportDetails swapchain = new SwapChainSupportDetails();

        swapchain.capabilities = VkSurfaceCapabilitiesKHR.create();
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, swapchain.capabilities);

        try (MemoryStack stack = stackPush()) {
            IntBuffer length = stack.ints(0);

            vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, length, null);
            if (length.get(0) != 0) {
                swapchain.formats = VkSurfaceFormatKHR.create(length.get(0));
                vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, length, swapchain.formats);
            }

            vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, length, null);
            if (length.get(0) != 0) {
                swapchain.presentModes = BufferUtils.createIntBuffer(length.get(0));
                vkGetPhysicalDeviceSurfacePresentModesKHR(
                        device, surface, length, swapchain.presentModes);
            }
        }

        return swapchain;
    }

    /// Logical device setup

    /** Creates a logical device with required features */
    private void setupLogicalDevice() {
        LOGGER.info("Setup logical device");

        try (MemoryStack stack = stackPush()) {
            FloatBuffer queuePriority = stack.floats(1.0f);

            int[] families = physicalDevice.indices.uniqueFamilies();

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

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.callocStack(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfo);
            createInfo.pEnabledFeatures(deviceFeatures);
            createInfo.ppEnabledExtensionNames(toPointerBuffer(DEVICE_EXTENSIONS, stack));

            if (DEBUG_MODE)
                createInfo.ppEnabledLayerNames(
                        toPointerBuffer(WANTED_VALIDATION_LAYERS_LIST, stack));

            PointerBuffer pDevice = stack.callocPointer(1);

            int result = vkCreateDevice(physicalDevice.device, createInfo, null, pDevice);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create VK logical device! Err: %x", -result));
            }

            device = new VkDevice(pDevice.get(0), physicalDevice.device, createInfo);

            PointerBuffer pQueue = stack.callocPointer(1);
            vkGetDeviceQueue(device, physicalDevice.indices.graphicsFamily, 0, pQueue);
            graphicsQueue = new VkQueue(pQueue.get(0), device);
            vkGetDeviceQueue(device, physicalDevice.indices.presentFamily, 0, pQueue);
            presentQueue = new VkQueue(pQueue.get(0), device);
        }
    }

    /// Shader setup

    /** Compiles shaders and caches them */
    private void setupShaders() {
        vertShader = ShaderBuf.getResource("shader", ShaderKind.VERTEX_SHADER);
        if (vertShader == null) throw new RuntimeException("Failed to load vertex shader!");

        fragShader = ShaderBuf.getResource("shader", ShaderKind.FRAGMENT_SHADER);
        if (fragShader == null) throw new RuntimeException("Failed to load fragment shader!");
    }

    /// Swapchain setup

    /** Sets up the swapchain required for rendering */
    private void setupSwapchain() {
        LOGGER.info("Setup swapchain");

        try (MemoryStack stack = stackPush()) {
            surfaceFormat = physicalDevice.swapchainSupport.chooseSurfaceFormat();
            int presentMode = physicalDevice.swapchainSupport.choosePresentMode();
            extent = physicalDevice.swapchainSupport.chooseExtent(window);
            int imageCount = physicalDevice.swapchainSupport.chooseImageCount();

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.callocStack(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(surface);
            createInfo.minImageCount(imageCount);
            createInfo.imageFormat(surfaceFormat.format());
            createInfo.imageColorSpace(surfaceFormat.colorSpace());
            createInfo.imageExtent(extent);
            createInfo.imageArrayLayers(1);
            // Render directly. For post-processing,
            // we may need VK_IMAGE_USAGE_TRANSFER_DST_BIT
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            // If we have separate queues, use concurrent mode which is easier to work with,
            // although slightly less efficient.
            if (graphicsQueue.address() != presentQueue.address()) {
                IntBuffer indices =
                        stack.ints(
                                physicalDevice.indices.graphicsFamily,
                                physicalDevice.indices.presentFamily);
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                createInfo.pQueueFamilyIndices(indices);
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            createInfo.preTransform(
                    physicalDevice.swapchainSupport.capabilities.currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);

            LongBuffer pSwapchain = stack.longs(0);

            int result = vkCreateSwapchainKHR(device, createInfo, null, pSwapchain);

            if (result != VK_SUCCESS)
                throw new RuntimeException(
                        String.format("Failed to create swapchain! Error: %x", -result));

            swapchain = pSwapchain.get(0);

            IntBuffer pImageCount = stack.ints(0);

            vkGetSwapchainImagesKHR(device, swapchain, pImageCount, null);

            LOGGER.info(String.format("%d", pImageCount.get(0)));

            LongBuffer pSwapchainImages = stack.mallocLong(pImageCount.get(0));

            vkGetSwapchainImagesKHR(device, swapchain, pImageCount, pSwapchainImages);

            swapchainImages =
                    IntStream.range(0, pImageCount.get(0))
                            .mapToLong(pSwapchainImages::get)
                            .toArray();
        }
    }

    /// Image view setup

    private void setupImageViews() {
        LOGGER.info("Setup image views");

        try (MemoryStack stack = stackPush()) {
            VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.callocStack(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            createInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
            createInfo.format(surfaceFormat.format());

            createInfo.components().r(VK_COMPONENT_SWIZZLE_IDENTITY);
            createInfo.components().g(VK_COMPONENT_SWIZZLE_IDENTITY);
            createInfo.components().b(VK_COMPONENT_SWIZZLE_IDENTITY);
            createInfo.components().a(VK_COMPONENT_SWIZZLE_IDENTITY);

            createInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            createInfo.subresourceRange().baseMipLevel(0);
            createInfo.subresourceRange().levelCount(1);
            createInfo.subresourceRange().baseArrayLayer(0);
            createInfo.subresourceRange().layerCount(1);

            LongBuffer imageView = stack.longs(0);

            swapchainImageViews =
                    Arrays.stream(swapchainImages)
                            .map(
                                    i -> {
                                        createInfo.image(i);
                                        int result =
                                                vkCreateImageView(
                                                        device, createInfo, null, imageView);
                                        if (result != VK_SUCCESS) {
                                            throw new RuntimeException(
                                                    String.format(
                                                            "Failed to create image view for %x! Error: %x",
                                                            i, -result));
                                        }
                                        return imageView.get(0);
                                    })
                            .toArray();
        }
    }

    /// Render pass setup

    private void setupRenderPass() {
        LOGGER.info("Setup render pass");

        try (MemoryStack stack = stackPush()) {
            VkAttachmentDescription.Buffer colorAttachment =
                    VkAttachmentDescription.callocStack(1, stack);
            colorAttachment.format(surfaceFormat.format());
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

            int result = vkCreateRenderPass(device, renderPassInfo, null, pRenderPass);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create render pass! Err: %x", -result));
            }

            renderPass = pRenderPass.get(0);
        }
    }

    /// Descriptor set setup

    private void setupDescriptorSetLayout() {
        LOGGER.info("Setup descriptor set layout");

        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer uboLayoutBinding =
                    VkDescriptorSetLayoutBinding.callocStack(1, stack);
            uboLayoutBinding.binding(0);
            uboLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            uboLayoutBinding.descriptorCount(1);
            uboLayoutBinding.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

            VkDescriptorSetLayoutCreateInfo layoutInfo =
                    VkDescriptorSetLayoutCreateInfo.callocStack(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.pBindings(uboLayoutBinding);

            LongBuffer pDescriptorSetLayout = stack.longs(0);

            int res = vkCreateDescriptorSetLayout(device, layoutInfo, null, pDescriptorSetLayout);

            if (res != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create descriptor set layout! Res: %x", -res));
            }

            descriptorSetLayout = pDescriptorSetLayout.get(0);
        }
    }

    /// Graphics pipeline setup

    private void setupGraphicsPipeline() {
        LOGGER.info("Setup graphics pipeline");

        Shader vertShader = Shader.getShader(this.vertShader.get(), device);

        if (vertShader == null) throw new RuntimeException("Failed to retrieve vertex shader!");

        Shader fragShader = Shader.getShader(this.fragShader.get(), device);

        if (fragShader == null) throw new RuntimeException("Failed to retrieve fragment shader!");

        try (MemoryStack stack = stackPush()) {
            // Programmable pipelines

            VkPipelineShaderStageCreateInfo.Buffer shaderStages =
                    VkPipelineShaderStageCreateInfo.callocStack(2, stack);

            VkPipelineShaderStageCreateInfo vertShaderStageInfo = shaderStages.get(0);
            vertShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            vertShaderStageInfo.stage(VK_SHADER_STAGE_VERTEX_BIT);
            vertShaderStageInfo.module(vertShader.getModule());
            vertShaderStageInfo.pName(stack.UTF8("main"));
            // We will need pSpecializationInfo here to configure constants

            VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(1);
            fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            fragShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
            fragShaderStageInfo.module(fragShader.getModule());
            fragShaderStageInfo.pName(stack.UTF8("main"));

            // Fixed function pipelines

            VkPipelineVertexInputStateCreateInfo vertexInputInfo =
                    VkPipelineVertexInputStateCreateInfo.callocStack(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            vertexInputInfo.pVertexBindingDescriptions(Vertex.getBindingDescription(stack));
            vertexInputInfo.pVertexAttributeDescriptions(Vertex.getAttributeDescriptions(stack));

            VkPipelineInputAssemblyStateCreateInfo inputAssembly =
                    VkPipelineInputAssemblyStateCreateInfo.callocStack(stack);
            inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
            inputAssembly.primitiveRestartEnable(false);

            VkViewport.Buffer viewport = VkViewport.callocStack(1, stack);
            viewport.x(0.0f);
            viewport.y(0.0f);
            viewport.width((float) extent.width());
            viewport.height((float) extent.height());
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);

            // Render entire viewport at once
            VkRect2D.Buffer scissor = VkRect2D.callocStack(1, stack);
            scissor.offset().x(0);
            scissor.offset().y(0);
            scissor.extent(extent);

            VkPipelineViewportStateCreateInfo viewportState =
                    VkPipelineViewportStateCreateInfo.callocStack(stack);
            viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            viewportState.pViewports(viewport);
            viewportState.pScissors(scissor);

            VkPipelineRasterizationStateCreateInfo rasterizer =
                    VkPipelineRasterizationStateCreateInfo.callocStack(stack);
            rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            rasterizer.depthClampEnable(false);
            rasterizer.rasterizerDiscardEnable(false);
            rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
            rasterizer.lineWidth(1.0f);
            rasterizer.cullMode(VK_CULL_MODE_BACK_BIT);
            rasterizer.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);

            // Used for shadowmaps, which we currently don't have...
            rasterizer.depthBiasEnable(false);

            // TODO: Enable MSAA once we check for features etc...
            VkPipelineMultisampleStateCreateInfo multisampling =
                    VkPipelineMultisampleStateCreateInfo.callocStack(stack);
            multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            multisampling.sampleShadingEnable(false);
            multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            // TODO: Depth blend with VkPipelineDepthStencilStateCreateInfo

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment =
                    VkPipelineColorBlendAttachmentState.callocStack(1, stack);
            colorBlendAttachment.colorWriteMask(
                    VK_COLOR_COMPONENT_R_BIT
                            | VK_COLOR_COMPONENT_G_BIT
                            | VK_COLOR_COMPONENT_B_BIT
                            | VK_COLOR_COMPONENT_A_BIT);
            colorBlendAttachment.blendEnable(false);

            VkPipelineColorBlendStateCreateInfo colorBlending =
                    VkPipelineColorBlendStateCreateInfo.callocStack(stack);
            colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(false);
            colorBlending.pAttachments(colorBlendAttachment);

            // TODO: Dynamic states

            VkPipelineLayoutCreateInfo pipelineLayoutInfo =
                    VkPipelineLayoutCreateInfo.callocStack(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            LongBuffer pDescriptorSetLayout = stack.longs(descriptorSetLayout);
            pipelineLayoutInfo.pSetLayouts(pDescriptorSetLayout);

            LongBuffer pPipelineLayout = stack.longs(0);

            int result = vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create pipeline layout! Err: %x", -result));
            }

            pipelineLayout = pPipelineLayout.get(0);

            // Actual pipeline!

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo =
                    VkGraphicsPipelineCreateInfo.callocStack(1, stack);
            pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            pipelineInfo.pStages(shaderStages);

            pipelineInfo.pVertexInputState(vertexInputInfo);
            pipelineInfo.pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState);
            pipelineInfo.pRasterizationState(rasterizer);
            pipelineInfo.pMultisampleState(multisampling);
            pipelineInfo.pColorBlendState(colorBlending);

            pipelineInfo.layout(pipelineLayout);
            pipelineInfo.renderPass(renderPass);
            pipelineInfo.subpass(0);

            // We don't have base pipeline
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);

            LongBuffer pPipeline = stack.longs(0);

            result =
                    vkCreateGraphicsPipelines(
                            device, VK_NULL_HANDLE, pipelineInfo, null, pPipeline);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create graphics pipeline! Err: %x", -result));
            }

            graphicsPipeline = pPipeline.get(0);
        }

        fragShader.free();
        vertShader.free();
    }

    /// Framebuffer setup

    private void setupFramebuffers() {
        LOGGER.info("Setup framebuffers");

        try (MemoryStack stack = stackPush()) {
            VkFramebufferCreateInfo createInfo = VkFramebufferCreateInfo.callocStack(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);

            createInfo.renderPass(renderPass);
            createInfo.width(extent.width());
            createInfo.height(extent.height());
            createInfo.layers(1);

            LongBuffer attachment = stack.longs(0);
            LongBuffer framebuffer = stack.longs(0);

            framebuffers =
                    Arrays.stream(swapchainImageViews)
                            .map(
                                    i -> {
                                        attachment.put(i);
                                        createInfo.pAttachments(attachment.rewind());
                                        int result =
                                                vkCreateFramebuffer(
                                                        device, createInfo, null, framebuffer);
                                        if (result != VK_SUCCESS) {
                                            throw new RuntimeException(
                                                    String.format(
                                                            "Failed to create framebuffer for %x! Error: %x",
                                                            i, -result));
                                        }
                                        return framebuffer.get(0);
                                    })
                            .toArray();
        }
    }

    /// Command pool setup

    private void setupCommandPool() {
        LOGGER.info("Setup command pool");

        try (MemoryStack stack = stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.callocStack(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(physicalDevice.indices.graphicsFamily);

            LongBuffer pCommandPool = stack.longs(0);

            int result = vkCreateCommandPool(device, poolInfo, null, pCommandPool);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create command pool! Err: %x", -result));
            }

            commandPool = pCommandPool.get(0);
        }
    }

    /// Create vertex and index buffers for rendering

    private void setupModelBuffers() {
        setupVertexBuffer();
        setupIndexBuffer();
    }

    private void setupUniformBuffers() {
        long size = UniformBufferObject.SIZEOF;
        uniformBuffers =
                IntStream.range(0, framebuffers.length)
                        .mapToObj(
                                __ ->
                                        createBuffer(
                                                size,
                                                VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                                                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                                        | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT))
                        .toArray(VulkanBuffer[]::new);
        uniformBufferObjects = new UniformBufferObject[uniformBuffers.length];
    }

    private void setupVertexBuffer() {
        LOGGER.info("Setup vertex buffer");

        try (MemoryStack stack = stackPush()) {

            long size = VERTICES.length * Vertex.SIZEOF;

            VulkanBuffer stagingBuffer =
                    createBuffer(
                            size,
                            VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                    | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

            PointerBuffer pData = stack.pointers(0);
            vkMapMemory(device, stagingBuffer.memory, 0, size, 0, pData);
            ByteBuffer byteBuffer = pData.getByteBuffer((int) size);
            for (Vertex v : VERTICES) {
                v.copyTo(byteBuffer);
            }
            vkUnmapMemory(device, stagingBuffer.memory);

            vertexBuffer =
                    createBuffer(
                            size,
                            VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            copyBuffer(stagingBuffer, vertexBuffer, size);

            stagingBuffer.destroyBuffer(device);
        }
    }

    private void setupIndexBuffer() {
        LOGGER.info("Setup index buffer");

        try (MemoryStack stack = stackPush()) {

            long size = INDICES.length * 2;

            VulkanBuffer stagingBuffer =
                    createBuffer(
                            size,
                            VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                    | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

            PointerBuffer pData = stack.pointers(0);
            vkMapMemory(device, stagingBuffer.memory, 0, size, 0, pData);
            ByteBuffer byteBuffer = pData.getByteBuffer((int) size);
            for (short i : INDICES) {
                byteBuffer.putShort(i);
            }
            vkUnmapMemory(device, stagingBuffer.memory);

            indexBuffer =
                    createBuffer(
                            size,
                            VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            copyBuffer(stagingBuffer, indexBuffer, size);

            stagingBuffer.destroyBuffer(device);
        }
    }

    private void copyBuffer(VulkanBuffer from, VulkanBuffer to, long size) {
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

            VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack);
            copyRegion.srcOffset(0);
            copyRegion.dstOffset(0);
            copyRegion.size(size);
            vkCmdCopyBuffer(commandBuffer, from.buffer, to.buffer, copyRegion);

            vkEndCommandBuffer(commandBuffer);

            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(pCommandBuffer);

            vkQueueSubmit(graphicsQueue, submitInfo, NULL);
            vkQueueWaitIdle(graphicsQueue);
        }
    }

    private VulkanBuffer createBuffer(long size, int usage, int properties) {
        VulkanBuffer ret = new VulkanBuffer();
        try (MemoryStack stack = stackPush()) {
            VkBufferCreateInfo createInfo = VkBufferCreateInfo.callocStack(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            createInfo.size(size);
            createInfo.usage(usage);
            createInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pBuffer = stack.longs(0);

            int res = vkCreateBuffer(device, createInfo, null, pBuffer);

            if (res != VK_SUCCESS) {
                throw new RuntimeException(String.format("Failed to create buffer! Ret: %x", -res));
            }

            ret.buffer = pBuffer.get(0);

            VkMemoryRequirements memoryRequirements = VkMemoryRequirements.callocStack(stack);
            vkGetBufferMemoryRequirements(device, ret.buffer, memoryRequirements);

            VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.callocStack(stack);
            allocateInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocateInfo.allocationSize(memoryRequirements.size());
            allocateInfo.memoryTypeIndex(
                    findMemoryType(memoryRequirements.memoryTypeBits(), properties));

            LongBuffer pBufferMemory = stack.longs(0);

            res = vkAllocateMemory(device, allocateInfo, null, pBufferMemory);

            if (res != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to allocate buffer memory! Ret: %x", -res));
            }

            ret.memory = pBufferMemory.get(0);

            res = vkBindBufferMemory(device, ret.buffer, ret.memory, 0);
        }
        return ret;
    }

    private int findMemoryType(int filterBits, int properties) {
        try (MemoryStack stack = stackPush()) {
            VkPhysicalDeviceMemoryProperties memProperties =
                    VkPhysicalDeviceMemoryProperties.callocStack(stack);
            vkGetPhysicalDeviceMemoryProperties(physicalDevice.device, memProperties);

            for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
                if ((filterBits & (1 << i)) != 0
                        && (memProperties.memoryTypes(i).propertyFlags() & properties)
                                == properties) {
                    return i;
                }
            }

            throw new RuntimeException("Failed to find suitable memory type!");
        }
    }

    /// Descriptor pool setup

    private void setupDescriptorPool() {
        LOGGER.info("Setup descriptor pool");

        try (MemoryStack stack = stackPush()) {
            VkDescriptorPoolSize.Buffer poolSize = VkDescriptorPoolSize.callocStack(1, stack);
            poolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            poolSize.descriptorCount(uniformBuffers.length);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.callocStack(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSize);
            poolInfo.maxSets(uniformBuffers.length);

            LongBuffer pDescriptorPool = stack.longs(0);

            int res = vkCreateDescriptorPool(device, poolInfo, null, pDescriptorPool);

            if (res != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create descriptor pool! Res: %x", -res));
            }

            descriptorPool = pDescriptorPool.get(0);
        }
    }

    /// Descriptor sets setup

    private void setupDescriptorSets() {
        LOGGER.info("Setup descriptor sets");

        try (MemoryStack stack = stackPush()) {
            LongBuffer setLayouts = stack.mallocLong(uniformBuffers.length);
            IntStream.range(0, uniformBuffers.length)
                    .mapToLong(__ -> descriptorSetLayout)
                    .forEach(setLayouts::put);
            setLayouts.rewind();

            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
            allocInfo.descriptorPool(descriptorPool);
            allocInfo.pSetLayouts(setLayouts);

            LongBuffer pDescriptorSets = stack.mallocLong(uniformBuffers.length);

            int res = vkAllocateDescriptorSets(device, allocInfo, pDescriptorSets);

            if (res != VK_SUCCESS)
                throw new RuntimeException(
                        String.format("Failed to create descriptor sets! Res: %x", -res));

            descriptorSets =
                    IntStream.range(0, uniformBuffers.length)
                            .mapToLong(pDescriptorSets::get)
                            .toArray();

            for (int i = 0; i < uniformBuffers.length; i++) {
                VkDescriptorBufferInfo.Buffer bufferInfo =
                        VkDescriptorBufferInfo.callocStack(1, stack);
                bufferInfo.offset(0);
                bufferInfo.range(UniformBufferObject.SIZEOF);

                VkWriteDescriptorSet.Buffer descriptorWrite =
                        VkWriteDescriptorSet.callocStack(1, stack);
                descriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                descriptorWrite.dstBinding(0);
                descriptorWrite.dstArrayElement(0);
                descriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                descriptorWrite.descriptorCount(1);
                descriptorWrite.pBufferInfo(bufferInfo);

                bufferInfo.buffer(uniformBuffers[i].buffer);
                descriptorWrite.dstSet(descriptorSets[i]);

                vkUpdateDescriptorSets(device, descriptorWrite, null);
            }
        }
    }

    /// Command buffer setup

    private void setupCommandBuffers() {
        LOGGER.info("Setup command buffers");

        try (MemoryStack stack = stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(commandPool);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(framebuffers.length);

            PointerBuffer buffers = stack.mallocPointer(allocInfo.commandBufferCount());

            int result = vkAllocateCommandBuffers(device, allocInfo, buffers);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create command buffers! Err: %x", -result));
            }

            commandBuffers =
                    IntStream.range(0, buffers.capacity())
                            .mapToObj(buffers::get)
                            .map(d -> new VkCommandBuffer(d, device))
                            .toArray(VkCommandBuffer[]::new);

            // Record the command buffers
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.callocStack(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
            renderPassInfo.renderPass(renderPass);

            renderPassInfo.renderArea().offset().clear();
            renderPassInfo.renderArea().extent(extent);

            VkClearValue.Buffer clearColor = VkClearValue.callocStack(1, stack);
            renderPassInfo.pClearValues(clearColor);

            int len = commandBuffers.length;

            for (int i = 0; i < len; i++) {
                long fb = framebuffers[i];
                VkCommandBuffer cb = commandBuffers[i];

                int res = vkBeginCommandBuffer(cb, beginInfo);

                if (res != VK_SUCCESS) {
                    String format =
                            String.format("Failed to begin recording command buffer! Err: %x", res);
                    throw new RuntimeException(format);
                }

                renderPassInfo.framebuffer(fb);

                // This is the beginning :)
                vkCmdBeginRenderPass(cb, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

                vkCmdBindPipeline(cb, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);

                LongBuffer vertexBuffers = stack.longs(vertexBuffer.buffer);
                LongBuffer offsets = stack.longs(0);
                vkCmdBindVertexBuffers(cb, 0, vertexBuffers, offsets);
                vkCmdBindIndexBuffer(cb, indexBuffer.buffer, 0, VK_INDEX_TYPE_UINT16);

                LongBuffer descriptorSet = stack.longs(descriptorSets[i]);
                vkCmdBindDescriptorSets(
                        cb,
                        VK_PIPELINE_BIND_POINT_GRAPHICS,
                        pipelineLayout,
                        0,
                        descriptorSet,
                        null);
                vkCmdDrawIndexed(cb, INDICES.length, 1, 0, 0, 0);

                vkCmdEndRenderPass(cb);

                // And this is the end
                res = vkEndCommandBuffer(cb);

                if (res != VK_SUCCESS) {
                    String format =
                            String.format("Failed to end recording command buffer! Err: %x", res);
                    throw new RuntimeException(format);
                }
            }
        }
    }

    /// Setup synchronization semaphores

    private void setupSyncObjects() {
        LOGGER.info("Setup sync objects");

        try (MemoryStack stack = stackPush()) {
            frames = new FrameContext[FRAMES_IN_FLIGHT];
            imagesInFlight = new long[commandBuffers.length];

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.callocStack(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.callocStack(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            IntStream.range(0, FRAMES_IN_FLIGHT)
                    .forEach(
                            i -> {
                                FrameContext ctx = new FrameContext();

                                LongBuffer pSync = stack.longs(0, 0, 0);

                                int res1 = vkCreateSemaphore(device, semaphoreInfo, null, pSync);
                                int res2 =
                                        vkCreateSemaphore(
                                                device, semaphoreInfo, null, pSync.position(1));
                                int res3 =
                                        vkCreateFence(device, fenceInfo, null, pSync.position(2));

                                if (res1 != VK_SUCCESS
                                        || res2 != VK_SUCCESS
                                        || res3 != VK_SUCCESS) {
                                    throw new RuntimeException(
                                            String.format(
                                                    "Failed to create semaphores! Err: %x %x",
                                                    -res1, -res2, -res2));
                                }

                                pSync.rewind();

                                ctx.imageAvailableSemaphore = pSync.get(0);
                                ctx.renderFinishedSemaphore = pSync.get(1);
                                ctx.inFlightFence = pSync.get(2);

                                frames[i] = ctx;
                            });
        }
    }

    /// Cleanup code

    /** Destroys debugMessenger if exists */
    private void destroyDebugMessanger() {
        if (debugMessenger == 0) return;
        vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
    }
}
