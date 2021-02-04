/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.Configuration.DEBUG;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;

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

    public static final Logger LOGGER = Logger.getLogger("render");

    public static final boolean DEBUG_MODE;

    static {
        String line = System.getenv("DEBUG_RENDERER");
        DEBUG_MODE = line != null && line.equals("true");
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

    /** Entrypoint of the app instance */
    public void run(int width, int height, String appName) {
        DEBUG.set(DEBUG_MODE);
        initWindow(width, height, appName);
        initVulkan(appName);
        if (DEBUG_MODE) {
            setupDebugLogging();
        }
        mainLoop();
        cleanup();
    }

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

    /** initialize Vulkan context */
    private void initVulkan(String appName) {
        LOGGER.info("Initialize VK Context");

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

    private void mainLoop() {
        LOGGER.info("Enter main loop");
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
        }
    }

    private void cleanup() {
        LOGGER.info("Cleanup");
        destroyDebugMessanger();
        vkDestroyInstance(instance, null);
        glfwDestroyWindow(window);
        glfwTerminate();
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

        String[] wantedLayers = {"VK_LAYER_KHRONOS_validation"};
        List<String> wantedList = Arrays.asList(wantedLayers);
        Set<String> wantedSet = new HashSet<>(wantedList);

        IntBuffer propertyCount = stack.ints(1);
        vkEnumerateInstanceLayerProperties(propertyCount, null);
        VkLayerProperties.Buffer properties =
                VkLayerProperties.mallocStack(propertyCount.get(0), stack);
        vkEnumerateInstanceLayerProperties(propertyCount, properties);

        boolean containsAll =
                wantedSet.isEmpty()
                        || properties.stream()
                                .map(VkLayerProperties::layerNameString)
                                .filter(wantedSet::remove)
                                .anyMatch(__ -> wantedSet.isEmpty());

        if (containsAll) {
            PointerBuffer buffer = stack.mallocPointer(wantedLayers.length);
            wantedList.stream().map(stack::UTF8).forEach(buffer::put);
            createInfo.ppEnabledLayerNames(buffer.rewind());
        } else {
            throw new RuntimeException("Some VK validation layers were not found!");
        }
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

    /** Destroys debugMessenger if exists */
    private void destroyDebugMessanger() {
        if (debugMessenger == 0) return;
        vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
    }
}
