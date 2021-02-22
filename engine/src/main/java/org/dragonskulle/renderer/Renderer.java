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
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.*;

public class Renderer {

    private long window;

    private VkInstance instance;
    private long debugMessenger;
    private long surface;

    private PhysicalDevice physicalDevice;

    private VkDevice device;
    private VkQueue graphicsQueue;
    private VkQueue presentQueue;

    private long commandPool;
    private long descriptorSetLayout;

    private TextureSamplerFactory samplerFactory;
    private TmpObjectState objectState;

    private VkSurfaceFormatKHR surfaceFormat;
    private VkExtent2D extent;
    private long swapchain;
    private long descriptorPool;
    private long renderPass;

    private ImageContext[] imageContexts;
    private FrameContext[] frameContexts;

    private int frameCounter = 0;

    private static final List<String> WANTED_VALIDATION_LAYERS_LIST =
            Arrays.asList("VK_LAYER_KHRONOS_validation");
    private static Set<String> DEVICE_EXTENSIONS =
            Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME).collect(toSet());

    public static final Logger LOGGER = Logger.getLogger("render");
    public static final boolean DEBUG_MODE = envBool("DEBUG_RENDERER", false);
    private static final String TARGET_GPU = envString("TARGET_GPU", null);

    private static final long UINT64_MAX = -1L;
    private static final int FRAMES_IN_FLIGHT = 4;

    private class FrameContext {
        private long imageAvailableSemaphore;
        private long renderFinishedSemaphore;
        private long inFlightFence;
    }

    private static class ImageContext {
        long descriptorSet;
        long imageView;
        long framebuffer;
        VkCommandBuffer commandBuffer;
        long inFlightFence;

        // TODO: Remove this
        UniformBufferObject ubo;
        VulkanBuffer buffer;
        VulkanImage textureImage;
        long textureImageView;

        private static final TextureMapping MAPPING =
                new TextureMapping(TextureFiltering.LINEAR, TextureWrapping.REPEAT);

        private ImageContext(
                Renderer renderer, long image, long descriptorSet, long commandBuffer) {
            this.descriptorSet = descriptorSet;
            this.imageView = renderer.createImageView(image);
            this.framebuffer = createFramebuffer(renderer);
            this.commandBuffer = new VkCommandBuffer(commandBuffer, renderer.device);

            ubo = new UniformBufferObject(new Matrix4f(), new Matrix4f(), new Matrix4f());
            ubo.view.lookAt(
                    new Vector3f(2.0f, 2.0f, 2.0f),
                    new Vector3f(0.0f, 0.0f, -0.05f),
                    new Vector3f(0.0f, 0.0f, 1.0f));
            ubo.proj.setPerspective(
                    45.0f,
                    (float) renderer.extent.width() / (float) renderer.extent.height(),
                    0.1f,
                    10.f,
                    true);
            ubo.proj.m11(-ubo.proj.m11());

            long size = UniformBufferObject.SIZEOF;
            buffer =
                    renderer.createBuffer(
                            size,
                            VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                    | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

            try (Resource<Texture> resource = Texture.getResource("test_cc0_texture.jpg")) {
                if (resource == null) throw new RuntimeException("Failed to load texture!");
                Texture texture = resource.get();
                textureImage = renderer.createTextureImage(texture);
            }
            textureImageView =
                    renderer.createImageView(textureImage.image, VK_FORMAT_R8G8B8A8_SRGB);
        }

        private void updateDescriptorSet(Renderer renderer) {
            try (MemoryStack stack = stackPush()) {
                VkDescriptorBufferInfo.Buffer bufferInfo =
                        VkDescriptorBufferInfo.callocStack(1, stack);
                bufferInfo.offset(0);
                bufferInfo.range(UniformBufferObject.SIZEOF);
                bufferInfo.buffer(buffer.buffer);

                VkDescriptorImageInfo.Buffer imageInfo =
                        VkDescriptorImageInfo.callocStack(1, stack);
                imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                imageInfo.imageView(textureImageView);
                imageInfo.sampler(renderer.samplerFactory.getSampler(MAPPING));

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

                vkUpdateDescriptorSets(renderer.device, descriptorWrites, null);
            }
        }

        private long createFramebuffer(Renderer renderer) {
            try (MemoryStack stack = stackPush()) {
                VkFramebufferCreateInfo createInfo = VkFramebufferCreateInfo.callocStack(stack);
                createInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);

                createInfo.renderPass(renderer.renderPass);
                createInfo.width(renderer.extent.width());
                createInfo.height(renderer.extent.height());
                createInfo.layers(1);

                LongBuffer attachment = stack.longs(imageView);
                LongBuffer framebuffer = stack.longs(0);

                createInfo.pAttachments(attachment);

                int result = vkCreateFramebuffer(renderer.device, createInfo, null, framebuffer);

                if (result != VK_SUCCESS) {
                    throw new RuntimeException(
                            String.format(
                                    "Failed to create framebuffer for %x! Error: %x",
                                    imageView, -result));
                }

                return framebuffer.get(0);
            }
        }

        private void updateUniformBuffer(Renderer renderer, float curtime) {
            ubo.model.rotation(curtime * 1.5f, 0.0f, 0.0f, 1.0f);

            try (MemoryStack stack = stackPush()) {
                PointerBuffer pData = stack.pointers(0);
                vkMapMemory(
                        renderer.device, buffer.memory, 0, UniformBufferObject.SIZEOF, 0, pData);
                ByteBuffer byteBuffer = pData.getByteBuffer(UniformBufferObject.SIZEOF);
                ubo.copyTo(byteBuffer);
                vkUnmapMemory(renderer.device, buffer.memory);
            }
        }

        private void free(VkDevice device) {
            vkDestroyFramebuffer(device, framebuffer, null);
            vkDestroyImageView(device, imageView, null);

            vkDestroyImageView(device, textureImageView, null);
            textureImage.destroyImage(device);

            buffer.destroyBuffer(device);
        }
    }

    private static class TmpObjectState {
        VulkanPipeline graphics;
        VulkanBuffer vertexBuffer;
        VulkanBuffer indexBuffer;

        private TmpObjectState(Renderer renderer) {
            Resource<ShaderBuf> vertShader =
                    ShaderBuf.getResource("shader", ShaderKind.VERTEX_SHADER);
            if (vertShader == null) throw new RuntimeException("Failed to load vertex shader!");

            Resource<ShaderBuf> fragShader =
                    ShaderBuf.getResource("shader", ShaderKind.FRAGMENT_SHADER);
            if (fragShader == null) throw new RuntimeException("Failed to load fragment shader!");

            graphics = renderer.createPipeline(vertShader.get(), fragShader.get());

            vertexBuffer = renderer.createVertexBuffer(VERTICES);
            indexBuffer = renderer.createIndexBuffer(INDICES);
        }

        private void free(VkDevice device) {
            graphics.free(device);
            vertexBuffer.destroyBuffer(device);
            indexBuffer.destroyBuffer(device);
        }

        private static Vertex[] VERTICES = {
            new Vertex(
                    new Vector2f(0.0f, 0.0f),
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    new Vector2f(0.0f, 0.0f)),
            new Vertex(
                    new Vector2f(-0.5f, 0.86603f),
                    new Vector3f(0.0f, 0.0f, 1.0f),
                    new Vector2f(-0.5f, 0.86603f)),
            new Vertex(
                    new Vector2f(0.5f, 0.86603f),
                    new Vector3f(0.0f, 1.0f, 0.0f),
                    new Vector2f(0.5f, 0.86603f)),
            new Vertex(
                    new Vector2f(1.0f, 0.0f),
                    new Vector3f(0.0f, 1.0f, 0.0f),
                    new Vector2f(1.0f, 0.0f)),
            new Vertex(
                    new Vector2f(0.5f, -0.86603f),
                    new Vector3f(0.0f, 1.0f, 0.0f),
                    new Vector2f(0.5f, -0.86603f)),
            new Vertex(
                    new Vector2f(-0.5f, -0.86603f),
                    new Vector3f(0.0f, 0.0f, 1.0f),
                    new Vector2f(-0.5f, -0.86603f)),
            new Vertex(
                    new Vector2f(-1.0f, 0.0f),
                    new Vector3f(0.0f, 0.0f, 1.0f),
                    new Vector2f(-1.0f, 0.0f)),
        };

        private static short[] INDICES = {1, 0, 2, 2, 0, 3, 3, 0, 4, 4, 0, 5, 5, 0, 6, 6, 0, 1};
    }

    public Renderer(String appName, long window) {
        this.window = window;
        instance = createInstance(appName);
        if (DEBUG_MODE) debugMessenger = createDebugLogger();
        surface = createSurface();
        physicalDevice = pickPhysicalDevice();
        device = createLogicalDevice();
        graphicsQueue = createGraphicsQueue();
        presentQueue = createPresentQueue();
        commandPool = createCommandPool();
        descriptorSetLayout = createDescriptorSetLayout();
        samplerFactory = new TextureSamplerFactory(device, physicalDevice);
        createSwapchainObjects();
        frameContexts = createFrameContexts(FRAMES_IN_FLIGHT);
    }

    // TODO: remove curtime
    public void render(Camera camera, List<Renderable> objects, float curtime) {
        // Right here we will hotload images as they come
        // Also handle vertex, and index buffers

        if (imageContexts == null) recreateSwapchain();

        try (MemoryStack stack = stackPush()) {
            FrameContext ctx = frameContexts[frameCounter];
            frameCounter = (frameCounter + 1) % FRAMES_IN_FLIGHT;

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
            final ImageContext image = imageContexts[imageIndex.get(0)];

            if (res == VK_ERROR_OUT_OF_DATE_KHR) {
                recreateSwapchain();
                return;
            }

            if (image.inFlightFence != 0)
                vkWaitForFences(device, image.inFlightFence, true, UINT64_MAX);

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
    }

    public void onResize() {
        recreateSwapchain();
    }

    public void free() {
        vkDeviceWaitIdle(device);
        for (FrameContext frame : frameContexts) {
            vkDestroySemaphore(device, frame.renderFinishedSemaphore, null);
            vkDestroySemaphore(device, frame.imageAvailableSemaphore, null);
            vkDestroyFence(device, frame.inFlightFence, null);
        }
        cleanupSwapchain();
        vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null);
        samplerFactory.free();
        vkDestroyCommandPool(device, commandPool, null);
        vkDestroyDevice(device, null);
        destroyDebugMessanger();
        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void recreateSwapchain() {
        if (imageContexts != null) {
            vkQueueWaitIdle(presentQueue);
            vkQueueWaitIdle(graphicsQueue);
            vkDeviceWaitIdle(device);

            cleanupSwapchain();
        }

        try (MemoryStack stack = stackPush()) {
            IntBuffer x = stack.ints(0);
            IntBuffer y = stack.ints(0);
            glfwGetFramebufferSize(window, x, y);
            LOGGER.info(String.format("%d %d", x.get(0), y.get(0)));
            if (x.get(0) == 0 || y.get(0) == 0) return;
        }

        physicalDevice.onRecreateSwapchain(surface);

        createSwapchainObjects();
    }

    private void cleanupSwapchain() {
        objectState.free(device);
        objectState = null;

        for (ImageContext ctx : imageContexts) {
            ctx.free(device);
        }

        try (MemoryStack stack = stackPush()) {
            vkFreeCommandBuffers(
                    device,
                    commandPool,
                    toPointerBuffer(
                            Arrays.stream(imageContexts)
                                    .map(d -> d.commandBuffer)
                                    .toArray(VkCommandBuffer[]::new),
                            stack));
        }

        imageContexts = null;

        vkDestroyDescriptorPool(device, descriptorPool, null);
        vkDestroyRenderPass(device, renderPass, null);
        vkDestroySwapchainKHR(device, swapchain, null);
    }

    /// Internal setup code

    private void createSwapchainObjects() {
        surfaceFormat = physicalDevice.swapchainSupport.chooseSurfaceFormat();
        extent = physicalDevice.swapchainSupport.chooseExtent(window);
        swapchain = createSwapchain();
        renderPass = createRenderPass();
        int imageCount = getImageCount();
        descriptorPool = createDescriptorPool(imageCount);
        imageContexts = createImageContexts(imageCount);
        objectState = new TmpObjectState(this);
        for (ImageContext img : imageContexts) {
            img.updateDescriptorSet(this);
            recordCommandBuffer(img, objectState);
        }
    }

    /// Instance setup

    private VkInstance createInstance(String appName) {
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
                            instance, createDebugLoggingInfo(stack), null, pDebugMessenger)
                    != VK_SUCCESS) {
                throw new RuntimeException("Failed to initialize debug messenger");
            }
            return pDebugMessenger.get();
        }
    }

    /// Setup window surface

    private long createSurface() {
        LOGGER.info("Setup surface");

        try (MemoryStack stack = stackPush()) {
            LongBuffer pSurface = stack.callocLong(1);
            int result = glfwCreateWindowSurface(instance, window, null, pSurface);
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
                PhysicalDevice.pickPhysicalDevice(instance, surface, TARGET_GPU, DEVICE_EXTENSIONS);
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
            deviceFeatures.samplerAnisotropy(physicalDevice.featureSupport.anisotropyEnable);

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

            VkDevice device = new VkDevice(pDevice.get(0), physicalDevice.device, createInfo);

            return device;
        }
    }

    /// Queue setup

    private VkQueue createGraphicsQueue() {

        try (MemoryStack stack = stackPush()) {
            PointerBuffer pQueue = stack.callocPointer(1);
            vkGetDeviceQueue(device, physicalDevice.indices.graphicsFamily, 0, pQueue);
            return new VkQueue(pQueue.get(0), device);
        }
    }

    private VkQueue createPresentQueue() {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pQueue = stack.callocPointer(1);
            vkGetDeviceQueue(device, physicalDevice.indices.presentFamily, 0, pQueue);
            return new VkQueue(pQueue.get(0), device);
        }
    }

    /// Command pool setup

    private long createCommandPool() {
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

            return pCommandPool.get(0);
        }
    }

    /// Descriptor set setup

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

            int res = vkCreateDescriptorSetLayout(device, layoutInfo, null, pDescriptorSetLayout);

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
            int presentMode = physicalDevice.swapchainSupport.choosePresentMode();
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

            return pSwapchain.get(0);
        }
    }

    /// Per swapchain image setup

    private int getImageCount() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pImageCount = stack.ints(0);

            vkGetSwapchainImagesKHR(device, swapchain, pImageCount, null);

            return pImageCount.get(0);
        }
    }

    private ImageContext[] createImageContexts(int imageCount) {
        try (MemoryStack stack = stackPush()) {
            // Allocate swapchain images
            LongBuffer pSwapchainImages = stack.mallocLong(imageCount);

            IntBuffer pImageCount = stack.ints(imageCount);

            vkGetSwapchainImagesKHR(device, swapchain, pImageCount, pSwapchainImages);

            // Allocate descriptor sets
            LongBuffer setLayouts = stack.mallocLong(imageCount);
            IntStream.range(0, imageCount)
                    .mapToLong(__ -> descriptorSetLayout)
                    .forEach(setLayouts::put);
            setLayouts.rewind();

            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
            allocInfo.descriptorPool(descriptorPool);
            allocInfo.pSetLayouts(setLayouts);

            LongBuffer pDescriptorSets = stack.mallocLong(imageCount);

            int res = vkAllocateDescriptorSets(device, allocInfo, pDescriptorSets);

            if (res != VK_SUCCESS)
                throw new RuntimeException(
                        String.format("Failed to create descriptor sets! Res: %x", -res));

            // Allocate command buffers

            VkCommandBufferAllocateInfo cmdAllocInfo =
                    VkCommandBufferAllocateInfo.callocStack(stack);
            cmdAllocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            cmdAllocInfo.commandPool(commandPool);
            cmdAllocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            cmdAllocInfo.commandBufferCount(imageCount);

            PointerBuffer buffers = stack.mallocPointer(cmdAllocInfo.commandBufferCount());

            int result = vkAllocateCommandBuffers(device, cmdAllocInfo, buffers);

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

            int res = vkCreateDescriptorPool(device, poolInfo, null, pDescriptorPool);

            if (res != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create descriptor pool! Res: %x", -res));
            }

            return pDescriptorPool.get(0);
        }
    }

    /// Image view setup

    private long createImageView(long image) {
        return createImageView(image, surfaceFormat.format());
    }

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

            int result = vkCreateImageView(device, createInfo, null, imageView);
            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format(
                                "Failed to create image view for %x! Error: %x", image, -result));
            }

            return imageView.get(0);
        }
    }

    /// Render pass setup

    private long createRenderPass() {
        LOGGER.info("Create render pass");

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

            return pRenderPass.get(0);
        }
    }

    /// Vertex and index buffers

    private VulkanBuffer createVertexBuffer(Vertex[] vertices) {
        LOGGER.info("Setup vertex buffer");

        try (MemoryStack stack = stackPush()) {

            long size = vertices.length * Vertex.SIZEOF;

            VulkanBuffer stagingBuffer =
                    createBuffer(
                            size,
                            VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                    | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

            PointerBuffer pData = stack.pointers(0);
            vkMapMemory(device, stagingBuffer.memory, 0, size, 0, pData);
            ByteBuffer byteBuffer = pData.getByteBuffer((int) size);
            for (Vertex v : vertices) {
                v.copyTo(byteBuffer);
            }
            vkUnmapMemory(device, stagingBuffer.memory);

            VulkanBuffer vertexBuffer =
                    createBuffer(
                            size,
                            VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            copyBuffer(stagingBuffer, vertexBuffer, size);

            stagingBuffer.destroyBuffer(device);

            return vertexBuffer;
        }
    }

    private VulkanBuffer createIndexBuffer(short[] indices) {
        LOGGER.info("Setup index buffer");

        try (MemoryStack stack = stackPush()) {

            long size = indices.length * 2;

            VulkanBuffer stagingBuffer =
                    createBuffer(
                            size,
                            VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                    | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

            PointerBuffer pData = stack.pointers(0);
            vkMapMemory(device, stagingBuffer.memory, 0, size, 0, pData);
            ByteBuffer byteBuffer = pData.getByteBuffer((int) size);
            for (short i : indices) {
                byteBuffer.putShort(i);
            }
            vkUnmapMemory(device, stagingBuffer.memory);

            VulkanBuffer indexBuffer =
                    createBuffer(
                            size,
                            VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            copyBuffer(stagingBuffer, indexBuffer, size);

            stagingBuffer.destroyBuffer(device);

            return indexBuffer;
        }
    }

    private VkCommandBuffer beginSingleUseCommandBuffer() {
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

    private void endSingleUseCommandBuffer(VkCommandBuffer commandBuffer) {
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

    private void copyBuffer(VulkanBuffer from, VulkanBuffer to, long size) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = beginSingleUseCommandBuffer();

            VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack);
            copyRegion.srcOffset(0);
            copyRegion.dstOffset(0);
            copyRegion.size(size);
            vkCmdCopyBuffer(commandBuffer, from.buffer, to.buffer, copyRegion);
            endSingleUseCommandBuffer(commandBuffer);
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

    /// Create a graphics pipeline

    private VulkanPipeline createPipeline(ShaderBuf vertShaderBuf, ShaderBuf fragShaderBuf) {
        LOGGER.info("Setup pipeline");

        Shader vertShader = Shader.getShader(vertShaderBuf, device);

        if (vertShader == null) throw new RuntimeException("Failed to retrieve vertex shader!");

        Shader fragShader = Shader.getShader(fragShaderBuf, device);

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

            long pipelineLayout = pPipelineLayout.get(0);

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

            fragShader.free();
            vertShader.free();

            return new VulkanPipeline(pPipeline.get(0), pipelineLayout);
        }
    }

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
            renderPassInfo.renderPass(renderPass);

            renderPassInfo.renderArea().offset().clear();
            renderPassInfo.renderArea().extent(extent);

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

    private VulkanImage createTextureImage(Texture texture) {
        LOGGER.info("Setup texture image");

        try (MemoryStack stack = stackPush()) {
            VulkanBuffer stagingBuffer =
                    createBuffer(
                            texture.size(),
                            VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                    | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

            PointerBuffer pData = stack.pointers(0);
            vkMapMemory(device, stagingBuffer.memory, 0, texture.size(), 0, pData);
            ByteBuffer byteBuffer = pData.getByteBuffer((int) texture.size());
            byteBuffer.put(texture.getBuffer());
            texture.getBuffer().rewind();
            byteBuffer.rewind();
            vkUnmapMemory(device, stagingBuffer.memory);

            VulkanImage textureImage =
                    createImage(
                            texture.getWidth(),
                            texture.getHeight(),
                            VK_FORMAT_R8G8B8A8_SRGB,
                            VK_IMAGE_TILING_OPTIMAL,
                            VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            transitionImageLayout(
                    textureImage.image,
                    VK_FORMAT_R8G8B8A8_SRGB,
                    VK_IMAGE_LAYOUT_UNDEFINED,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

            copyBufferToImage(stagingBuffer, textureImage, texture.getWidth(), texture.getHeight());

            transitionImageLayout(
                    textureImage.image,
                    VK_FORMAT_R8G8B8A8_SRGB,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

            stagingBuffer.destroyBuffer(device);
            return textureImage;
        }
    }

    private void transitionImageLayout(long image, long format, int oldLayout, int newLayout) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = beginSingleUseCommandBuffer();

            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.oldLayout(oldLayout);
            barrier.newLayout(newLayout);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.image(image);
            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().levelCount(1);
            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().layerCount(1);

            int srcStage = 0;
            int dstStage = 0;

            if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED
                    && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

                srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            } else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL
                    && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

                srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
                dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            } else {
                LOGGER.severe("Unsupported layout transition!");
            }

            vkCmdPipelineBarrier(commandBuffer, srcStage, dstStage, 0, null, null, barrier);

            endSingleUseCommandBuffer(commandBuffer);
        }
    }

    private void copyBufferToImage(VulkanBuffer buffer, VulkanImage image, int width, int height) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = beginSingleUseCommandBuffer();

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.callocStack(1, stack);

            // offsets set to 0

            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(0);
            region.imageSubresource().layerCount(1);
            region.imageSubresource().baseArrayLayer(0);

            region.imageExtent().width(width);
            region.imageExtent().height(height);
            region.imageExtent().depth(1);

            vkCmdCopyBufferToImage(
                    commandBuffer,
                    buffer.buffer,
                    image.image,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    region);

            endSingleUseCommandBuffer(commandBuffer);
        }
    }

    private VulkanImage createImage(
            int width, int height, int format, int tiling, int usage, int properties) {
        try (MemoryStack stack = stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.callocStack(stack);
            imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageInfo.imageType(VK_IMAGE_TYPE_2D);
            imageInfo.extent().width(width);
            imageInfo.extent().height(height);
            imageInfo.extent().depth(1);
            imageInfo.mipLevels(1);
            imageInfo.arrayLayers(1);
            imageInfo.format(format);
            imageInfo.tiling(tiling);
            imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.usage(usage);
            imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            imageInfo.samples(VK_SAMPLE_COUNT_1_BIT);

            LongBuffer pImage = stack.longs(0);

            int res = vkCreateImage(device, imageInfo, null, pImage);

            if (res != VK_SUCCESS)
                throw new RuntimeException(String.format("Failed to create image! Res: %x", -res));

            VulkanImage ret = new VulkanImage();

            ret.image = pImage.get(0);

            VkMemoryRequirements memoryRequirements = VkMemoryRequirements.callocStack(stack);
            vkGetImageMemoryRequirements(device, ret.image, memoryRequirements);

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

            res = vkBindImageMemory(device, ret.image, ret.memory, 0);

            return ret;
        }
    }

    /// Cleanup code

    /** Destroys debugMessenger if exists */
    private void destroyDebugMessanger() {
        if (debugMessenger == 0) return;
        vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
    }
}
