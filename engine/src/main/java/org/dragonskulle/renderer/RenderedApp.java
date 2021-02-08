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
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.var;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
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

    public static final Logger LOGGER = Logger.getLogger("render");

    public static final boolean DEBUG_MODE;
    private static final String TARGET_GPU = envString("TARGET_GPU", null);

    private static final String[] WANTED_VALIDATION_LAYERS = {"VK_LAYER_KHRONOS_validation"};
    private static final List<String> WANTED_VALIDATION_LAYERS_LIST =
            Arrays.asList(WANTED_VALIDATION_LAYERS);

    private static Set<String> DEVICE_EXTENSIONS =
            Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME).collect(toSet());

    static {
        String line = System.getenv("DEBUG_RENDERER");
        DEBUG_MODE = line != null && line.equals("true");
    }

    private class PhysicalDevice implements Comparable<PhysicalDevice> {
        VkPhysicalDevice device;
        VkPhysicalDeviceProperties properties;
        VkPhysicalDeviceFeatures features;
        SwapChainSupportDetails swapchainSupport;

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
            var properties = VkExtensionProperties.mallocStack(propertyCount.get(0), stack);
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
            for (int i = 0;
                    presentMode != VK_PRESENT_MODE_MAILBOX_KHR && i < presentModes.capacity();
                    i++) {
                int pm = presentModes.get(i);
                if (pm == VK_PRESENT_MODE_MAILBOX_KHR || pm == VK_PRESENT_MODE_FIFO_KHR)
                    presentMode = pm;
            }

            return presentMode;
        }

        /** Choose a compatible resolution, targetting current window resolution */
        public VkExtent2D chooseExtent(long window, MemoryStack stack) {
            IntBuffer res = stack.ints(0, 0);
            glfwGetFramebufferSize(window, res, res.position(1));

            VkExtent2D extent = VkExtent2D.mallocStack(stack);
            extent.set(
                    Integer.max(
                            Integer.min(res.get(0), capabilities.maxImageExtent().width()),
                            capabilities.minImageExtent().width()),
                    Integer.max(
                            Integer.min(res.get(1), capabilities.maxImageExtent().height()),
                            capabilities.minImageExtent().height()));

            LOGGER.info(String.format("Extent: %dx%d", extent.width(), extent.height()));

            return extent;
        }

        public int chooseImageCount() {
            int imageCount = capabilities.minImageCount() + 1;
            if (capabilities.maxImageCount() > 0 && imageCount > capabilities.maxImageCount())
                return capabilities.maxImageCount();
            else return imageCount;
        }
    }

    private static String envString(String key, String defaultVal) {
        String envLine = System.getenv(key);
        return envLine == null ? defaultVal : envLine;
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
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
        }
    }

    private void cleanup() {
        LOGGER.info("Cleanup");
        for (long imageView : swapchainImageViews) {
            vkDestroyImageView(device, imageView, null);
        }
        vkDestroySwapchainKHR(device, swapchain, null);
        vkDestroyDevice(device, null);
        destroyDebugMessanger();
        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    /// Setup code

    /** Creates a GLFW window */
    private void initWindow(int width, int height, String appName) {
        LOGGER.info("Initialize GLFW window");

        if (!glfwInit()) {
            throw new RuntimeException("Cannot initialize GLFW");
        }

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        window = glfwCreateWindow(width, height, appName, NULL, NULL);

        if (window == NULL) {
            throw new RuntimeException("Cannot create window");
        }
    }

    /** initialize Vulkan context. Throw on error */
    private void initVulkan(String appName) {
        LOGGER.info("Initialize VK Context");

        try (MemoryStack stack = stackPush()) {
            setupInstance(appName, stack);
            if (DEBUG_MODE) {
                setupDebugLogging();
            }
            setupSurface(stack);
            setupPhysicalDevice(stack);
            setupLogicalDevice(stack);
            setupSwapchain(stack);
            setupImageViews(stack);
            setupGraphicsPipeline(stack);
        }
    }

    /// Instance setup

    private void setupInstance(String appName, MemoryStack stack) {
        // Prepare basic Vulkan App information
        var appInfo = VkApplicationInfo.callocStack(stack);

        appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
        appInfo.pApplicationName(stack.UTF8Safe(appName));
        appInfo.applicationVersion(VK_MAKE_VERSION(0, 0, 1));
        appInfo.pEngineName(stack.UTF8Safe("DragonSkulle Engine"));
        appInfo.engineVersion(VK_MAKE_VERSION(0, 0, 1));
        appInfo.apiVersion(VK_API_VERSION_1_0);

        // Prepare a Vulkan instance information
        var createInfo = VkInstanceCreateInfo.callocStack(stack);

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
     * <p>Throws if the layers were not available
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
        var debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.callocStack(stack);

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
        MemoryStack stack = MemoryStack.stackGet();
        LongBuffer pDebugMessenger = stack.longs(0);
        if (vkCreateDebugUtilsMessengerEXT(
                        instance, createDebugLoggingInfo(stack), null, pDebugMessenger)
                != VK_SUCCESS) {
            throw new RuntimeException("Failed to initialize debug messenger");
        }
        debugMessenger = pDebugMessenger.get();
    }

    /// Setup window surface

    private void setupSurface(MemoryStack stack) {
        LongBuffer pSurface = stack.callocLong(1);
        int result = glfwCreateWindowSurface(instance, window, null, pSurface);
        if (result != VK_SUCCESS) {
            throw new RuntimeException(
                    String.format("Failed to create windows surface! %x", -result));
        }
        surface = pSurface.get(0);
    }

    /// Physical device setup

    /** Sets up one physical device for use */
    private void setupPhysicalDevice(MemoryStack stack) {
        LOGGER.info("Setup physical device");
        physicalDevice = pickPhysicalDevice(TARGET_GPU, stack);
        if (physicalDevice == null) {
            throw new RuntimeException("Failed to find compatible GPU!");
        }
        physicalDevice.swapchainSupport.chooseSurfaceFormat();
        LOGGER.info(String.format("Picked GPU: %s", physicalDevice.properties.deviceNameString()));
    }

    /** Picks a physical device with required features */
    private PhysicalDevice pickPhysicalDevice(String targetDevice, MemoryStack stack) {
        PhysicalDevice[] devices = enumeratePhysicalDevices(stack);

        if (devices.length == 0) return null;
        else if (targetDevice == null) return devices[0];

        for (PhysicalDevice d : devices) {
            if (d.properties.deviceNameString().contains(targetDevice)) {
                return d;
            }
        }

        return null;
    }

    /** Collect all compatible physical GPUs into an array, sorted by decreasing score */
    private PhysicalDevice[] enumeratePhysicalDevices(MemoryStack stack) {
        IntBuffer physDevCount = stack.ints(0);
        vkEnumeratePhysicalDevices(instance, physDevCount, null);
        PointerBuffer devices = stack.mallocPointer(physDevCount.get(0));
        vkEnumeratePhysicalDevices(instance, physDevCount, devices);

        return java.util.stream.IntStream.range(0, devices.capacity())
                .mapToObj(devices::get)
                .map(d -> new VkPhysicalDevice(d, instance))
                .map(d -> collectPhysicalDeviceInfo(d, stack))
                .filter(d -> isPhysicalDeviceSuitable(d, stack))
                .sorted()
                .toArray(PhysicalDevice[]::new);
    }

    /** check whether the device in question is suitable for us */
    private boolean isPhysicalDeviceSuitable(PhysicalDevice device, MemoryStack stack) {
        return device.indices.isComplete()
                && device.getDeviceExtensionProperties(stack).stream()
                        .map(VkExtensionProperties::extensionNameString)
                        .collect(toSet())
                        .containsAll(DEVICE_EXTENSIONS)
                && device.swapchainSupport.isAdequate();
    }

    /** Gathers information about physical device and stores it on `PhysicalDevice` */
    private PhysicalDevice collectPhysicalDeviceInfo(VkPhysicalDevice device, MemoryStack stack) {
        PhysicalDevice physdev = new PhysicalDevice(device);

        physdev.properties = VkPhysicalDeviceProperties.callocStack(stack);
        physdev.features = VkPhysicalDeviceFeatures.callocStack(stack);

        vkGetPhysicalDeviceProperties(device, physdev.properties);
        vkGetPhysicalDeviceFeatures(device, physdev.features);

        physdev.score = 0;

        // Prioritize dedicated graphics cards
        if (physdev.properties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU)
            physdev.score += 5000;

        // Check maximum texture sizes, prioritize largest
        physdev.score += physdev.properties.limits().maxImageDimension2D();

        QueueFamilyIndices indices = new QueueFamilyIndices();

        VkQueueFamilyProperties.Buffer buf = getQueueFamilyProperties(device, stack);

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
        physdev.swapchainSupport = getSwapchainSupport(device, stack);

        return physdev;
    }

    /** Utility for retrieving VkQueueFamilyProperties list */
    private VkQueueFamilyProperties.Buffer getQueueFamilyProperties(
            VkPhysicalDevice device, MemoryStack stack) {
        IntBuffer length = stack.ints(0);
        vkGetPhysicalDeviceQueueFamilyProperties(device, length, null);
        var props = VkQueueFamilyProperties.callocStack(length.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(device, length, props);
        return props;
    }

    private SwapChainSupportDetails getSwapchainSupport(
            VkPhysicalDevice device, MemoryStack stack) {
        SwapChainSupportDetails swapchain = new SwapChainSupportDetails();

        swapchain.capabilities = VkSurfaceCapabilitiesKHR.mallocStack(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, swapchain.capabilities);

        IntBuffer length = stack.ints(0);

        vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, length, null);
        if (length.get(0) != 0) {
            swapchain.formats = VkSurfaceFormatKHR.callocStack(length.get(0), stack);
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, length, swapchain.formats);
        }

        vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, length, null);
        if (length.get(0) != 0) {
            swapchain.presentModes = stack.callocInt(length.get(0));
            vkGetPhysicalDeviceSurfacePresentModesKHR(
                    device, surface, length, swapchain.presentModes);
        }

        return swapchain;
    }

    /// Logical device setup

    /** Creates a logical device with required features */
    private void setupLogicalDevice(MemoryStack stack) {
        LOGGER.info("Setup logical device");

        FloatBuffer queuePriority = stack.floats(1.0f);

        int[] families = physicalDevice.indices.uniqueFamilies();

        var queueCreateInfo = VkDeviceQueueCreateInfo.callocStack(families.length, stack);

        IntStream.range(0, families.length)
                .forEach(
                        i -> {
                            queueCreateInfo
                                    .get(i)
                                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                            queueCreateInfo.get(i).queueFamilyIndex(families[i]);
                            queueCreateInfo.get(i).pQueuePriorities(queuePriority);
                        });

        var deviceFeatures = VkPhysicalDeviceFeatures.callocStack(stack);

        var createInfo = VkDeviceCreateInfo.callocStack(stack);
        createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
        createInfo.pQueueCreateInfos(queueCreateInfo);
        createInfo.pEnabledFeatures(deviceFeatures);
        createInfo.ppEnabledExtensionNames(toPointerBuffer(DEVICE_EXTENSIONS, stack));

        if (DEBUG_MODE)
            createInfo.ppEnabledLayerNames(toPointerBuffer(WANTED_VALIDATION_LAYERS_LIST, stack));

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

    /// Swapchain setup

    /** Sets up the swapchain required for rendering */
    void setupSwapchain(MemoryStack stack) {
        LOGGER.info("Setup swapchain");

        surfaceFormat = physicalDevice.swapchainSupport.chooseSurfaceFormat();
        int presentMode = physicalDevice.swapchainSupport.choosePresentMode();
        extent = physicalDevice.swapchainSupport.chooseExtent(window, stack);
        int imageCount = physicalDevice.swapchainSupport.chooseImageCount();

        var createInfo = VkSwapchainCreateInfoKHR.callocStack(stack);
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

        createInfo.preTransform(physicalDevice.swapchainSupport.capabilities.currentTransform());
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
                IntStream.range(0, pImageCount.get(0)).mapToLong(pSwapchainImages::get).toArray();
    }

    /// Image view setup

    private void setupImageViews(MemoryStack stack) {
        LOGGER.info("Setup image views");

        var createInfo = VkImageViewCreateInfo.callocStack(stack);
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
                                            vkCreateImageView(device, createInfo, null, imageView);
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

    /// Graphics pipeline setup

    private void setupGraphicsPipeline(MemoryStack stack) {
        LOGGER.info("Setup graphics pipeline");
        Shader vertShader = Shader.getShader("shaderc/vert.spv", device);
        Shader fragShader = Shader.getShader("shaderc/frag.spv", device);

        var shaderStages = VkPipelineShaderStageCreateInfo.callocStack(2, stack);

        VkPipelineShaderStageCreateInfo vertShaderStageInfo = shaderStages.get(0);
        vertShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
        vertShaderStageInfo.stage(VK_SHADER_STAGE_VERTEX_BIT);
        vertShaderStageInfo.module(vertShader.getModule());
        vertShaderStageInfo.pName(stack.UTF8("main"));
        // We will need pSpecializationInfo here to configure constants

        VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(0);
        fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
        fragShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
        fragShaderStageInfo.module(fragShader.getModule());
        fragShaderStageInfo.pName(stack.UTF8("main"));

        fragShader.free();
        vertShader.free();
    }

    /// Cleanup code

    /** Destroys debugMessenger if exists */
    private void destroyDebugMessanger() {
        if (debugMessenger == 0) return;
        vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
    }
}
