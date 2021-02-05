/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.Configuration.DEBUG;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;

import com.codepoetics.protonpack.StreamUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

public class RenderedApp {

    @Getter private long window;

    private VkInstance instance;
    private long debugMessenger;

    private PhysicalDevice physicalDevice;
    private VkDevice device;
    private VkQueue graphicsQueue;

    public static final Logger LOGGER = Logger.getLogger("render");

    public static final boolean DEBUG_MODE;
    private static final String TARGET_GPU = envString("TARGET_GPU", null);

    private static final String[] WANTED_VALIDATION_LAYERS = {"VK_LAYER_KHRONOS_validation"};
    private static final List<String> WANTED_VALIDATION_LAYERS_LIST =
            Arrays.asList(WANTED_VALIDATION_LAYERS);

    static {
        String line = System.getenv("DEBUG_RENDERER");
        DEBUG_MODE = line != null && line.equals("true");
    }

    private class PhysicalDevice implements Comparable<PhysicalDevice> {
        VkPhysicalDevice device;
        VkPhysicalDeviceProperties properties;
        VkPhysicalDeviceFeatures features;

        int score;
        QueueFamilyIndices indices;

        PhysicalDevice(VkPhysicalDevice d) {
            device = d;
        }

        @Override
        public int compareTo(PhysicalDevice other) {
            return score == other.score ? 0 : score < other.score ? 1 : -1;
        }
    }

    private class QueueFamilyIndices {
        Integer graphicsFamily;

        QueueFamilyIndices(VkQueueFamilyProperties.Buffer buf) {
            StreamUtils.zipWithIndex(buf.stream().map(VkQueueFamilyProperties::queueFlags))
                    .takeWhile(__ -> !isComplete())
                    .forEach(
                            i -> {
                                if ((i.getValue() & VK_QUEUE_GRAPHICS_BIT) != 0)
                                    graphicsFamily = Integer.valueOf((int) i.getIndex());
                            });
        }

        boolean isComplete() {
            return graphicsFamily != null;
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
        vkDestroyDevice(device, null);
        destroyDebugMessanger();
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
            setupPhysicalDevice(stack);
            setupLogicalDevice(stack);
        }
    }

    /// Instance setup

    private void setupInstance(String appName, MemoryStack stack) {
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
            createInfo.ppEnabledLayerNames(validationLayersToPointerBuffer(stack));
        } else {
            throw new RuntimeException("Some VK validation layers were not found!");
        }
    }

    /** Utility for retrieving a list of all extensions */
    private PointerBuffer validationLayersToPointerBuffer(MemoryStack stack) {
        PointerBuffer buffer = stack.mallocPointer(WANTED_VALIDATION_LAYERS.length);
        WANTED_VALIDATION_LAYERS_LIST.stream().map(stack::UTF8).forEach(buffer::put);
        return buffer.rewind();
    }

    /** Utility for retrieving instance VkLayerProperties list */
    private VkLayerProperties.Buffer getInstanceLayerProperties(MemoryStack stack) {
        IntBuffer propertyCount = stack.ints(1);
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
        MemoryStack stack = MemoryStack.stackGet();
        LongBuffer pDebugMessenger = stack.longs(0);
        if (vkCreateDebugUtilsMessengerEXT(
                        instance, createDebugLoggingInfo(stack), null, pDebugMessenger)
                != VK_SUCCESS) {
            throw new RuntimeException("Failed to initialize debug messenger");
        }
        debugMessenger = pDebugMessenger.get();
    }

    /// Physical device setup

    /** Sets up one physical device for use */
    private void setupPhysicalDevice(MemoryStack stack) {
        LOGGER.info("Setup physical device");
        physicalDevice = pickPhysicalDevice(TARGET_GPU, stack);
        if (physicalDevice == null) {
            throw new RuntimeException("Failed to find compatible GPU!");
        }
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
                .filter(this::isPhysicalDeviceSuitable)
                .sorted()
                .toArray(PhysicalDevice[]::new);
    }

    /** check whether the device in question is suitable for us */
    private boolean isPhysicalDeviceSuitable(PhysicalDevice device) {
        return device.indices.isComplete();
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

        physdev.indices = new QueueFamilyIndices(getQueueFamilyProperties(device, stack));

        return physdev;
    }

    /** Utility for retrieving VkQueueFamilyProperties list */
    private VkQueueFamilyProperties.Buffer getQueueFamilyProperties(
            VkPhysicalDevice device, MemoryStack stack) {
        IntBuffer length = stack.ints(0);
        vkGetPhysicalDeviceQueueFamilyProperties(device, length, null);
        VkQueueFamilyProperties.Buffer props =
                VkQueueFamilyProperties.callocStack(length.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(device, length, props);
        return props;
    }

    /// Logical device setup

    /** Creates a logical device with required features */
    private void setupLogicalDevice(MemoryStack stack) {
        LOGGER.info("Setup logical device");
        VkDeviceQueueCreateInfo.Buffer queueCreateInfo =
                VkDeviceQueueCreateInfo.callocStack(1, stack);
        queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
        queueCreateInfo.queueFamilyIndex(physicalDevice.indices.graphicsFamily);
        FloatBuffer queuePriority = stack.floats(1.0f);
        queueCreateInfo.pQueuePriorities(queuePriority);

        VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.callocStack(stack);

        VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.callocStack(stack);
        createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
        createInfo.pQueueCreateInfos(queueCreateInfo);
        createInfo.pEnabledFeatures(deviceFeatures);

        if (DEBUG_MODE) createInfo.ppEnabledLayerNames(validationLayersToPointerBuffer(stack));

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
    }

    /// Cleanup code

    /** Destroys debugMessenger if exists */
    private void destroyDebugMessanger() {
        if (debugMessenger == 0) return;
        vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
    }
}
