/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.IntBuffer;
import java.util.*;
import java.util.stream.IntStream;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

/**
 * Describes a physical vulkan device, and the features it supports
 *
 * @author Aurimas Blažulionis
 */
class PhysicalDevice implements Comparable<PhysicalDevice> {
    VkPhysicalDevice device;
    SwapchainSupportDetails swapchainSupport;
    FeatureSupportDetails featureSupport;
    String deviceName;

    int score;
    QueueFamilyIndices indices;

    /** Describes indices used for various device queues */
    static class QueueFamilyIndices {
        Integer graphicsFamily;
        Integer presentFamily;

        boolean isComplete() {
            return graphicsFamily != null && presentFamily != null;
        }

        int[] uniqueFamilies() {
            return IntStream.of(graphicsFamily, presentFamily).distinct().toArray();
        }
    }

    /** Describes physical graphics feature support */
    static class FeatureSupportDetails {
        boolean anisotropyEnable;
        float maxAnisotropy;

        public boolean isSuitable() {
            return true;
        }
    }

    /** Gathers information about physical device and stores it on `PhysicalDevice` */
    private PhysicalDevice(VkPhysicalDevice device, long surface) {
        this.device = device;

        try (MemoryStack stack = stackPush()) {
            VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.callocStack(stack);
            VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.callocStack(stack);

            vkGetPhysicalDeviceProperties(device, properties);
            vkGetPhysicalDeviceFeatures(device, features);

            this.deviceName = properties.deviceNameString();

            this.featureSupport = new FeatureSupportDetails();
            this.featureSupport.anisotropyEnable = features.samplerAnisotropy();
            this.featureSupport.maxAnisotropy = properties.limits().maxSamplerAnisotropy();

            this.score = 0;

            // Prioritize dedicated graphics cards
            if (properties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) this.score += 5000;

            // Check maximum texture sizes, prioritize largest
            this.score += properties.limits().maxImageDimension2D();

            QueueFamilyIndices indices = new QueueFamilyIndices();

            VkQueueFamilyProperties.Buffer buf = getQueueFamilyProperties(device);

            IntBuffer presentSupport = stack.ints(0);

            int capacity = buf.capacity();

            for (int i = 0; i < capacity && !indices.isComplete(); i++) {
                int queueFlags = buf.get(i).queueFlags();

                if ((queueFlags & VK_QUEUE_GRAPHICS_BIT) != 0)
                    indices.graphicsFamily = Integer.valueOf(i);

                vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, presentSupport);

                if (presentSupport.get(0) == VK_TRUE) {
                    indices.presentFamily = i;
                }
            }

            this.indices = indices;
            this.swapchainSupport = new SwapchainSupportDetails(device, surface);
        }
    }

    @Override
    public int compareTo(PhysicalDevice other) {
        return score == other.score ? 0 : score < other.score ? 1 : -1;
    }

    /** Update swapchain support details */
    public void onRecreateSwapchain(long surface) {
        swapchainSupport = new SwapchainSupportDetails(device, surface);
    }

    /** Find a suitable memory type for the GPU */
    public int findMemoryType(int filterBits, int properties) {
        try (MemoryStack stack = stackPush()) {
            VkPhysicalDeviceMemoryProperties memProperties =
                    VkPhysicalDeviceMemoryProperties.callocStack(stack);
            vkGetPhysicalDeviceMemoryProperties(device, memProperties);

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

    /** Picks a physical device with required features */
    public static PhysicalDevice pickPhysicalDevice(
            VkInstance instance, long surface, String targetDevice, Set<String> neededExtensions) {
        PhysicalDevice[] devices = enumeratePhysicalDevices(instance, surface, neededExtensions);

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
    private static PhysicalDevice[] enumeratePhysicalDevices(
            VkInstance instance, long surface, Set<String> extensions) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer physDevCount = stack.ints(0);
            vkEnumeratePhysicalDevices(instance, physDevCount, null);
            PointerBuffer devices = stack.mallocPointer(physDevCount.get(0));
            vkEnumeratePhysicalDevices(instance, physDevCount, devices);

            return IntStream.range(0, devices.capacity())
                    .mapToObj(devices::get)
                    .map(d -> new VkPhysicalDevice(d, instance))
                    .map(d -> new PhysicalDevice(d, surface))
                    .filter(d -> isPhysicalDeviceSuitable(d, extensions))
                    .sorted()
                    .toArray(PhysicalDevice[]::new);
        }
    }

    private VkExtensionProperties.Buffer getDeviceExtensionProperties(MemoryStack stack) {
        IntBuffer propertyCount = stack.ints(0);
        vkEnumerateDeviceExtensionProperties(device, (String) null, propertyCount, null);
        VkExtensionProperties.Buffer properties =
                VkExtensionProperties.mallocStack(propertyCount.get(0), stack);
        vkEnumerateDeviceExtensionProperties(device, (String) null, propertyCount, properties);
        return properties;
    }

    /** check whether the device in question is suitable for us */
    private static boolean isPhysicalDeviceSuitable(PhysicalDevice device, Set<String> extensions) {
        try (MemoryStack stack = stackPush()) {
            return device.indices.isComplete()
                    && device.featureSupport.isSuitable()
                    && device.getDeviceExtensionProperties(stack).stream()
                            .map(VkExtensionProperties::extensionNameString)
                            .collect(toSet())
                            .containsAll(extensions)
                    && device.swapchainSupport.isAdequate();
        }
    }

    /** Utility for retrieving VkQueueFamilyProperties list */
    private static VkQueueFamilyProperties.Buffer getQueueFamilyProperties(
            VkPhysicalDevice device) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer length = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(device, length, null);
            VkQueueFamilyProperties.Buffer props =
                    VkQueueFamilyProperties.callocStack(length.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(device, length, props);
            return props;
        }
    }
}
