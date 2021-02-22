/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;

import com.codepoetics.protonpack.StreamUtils;
import java.nio.IntBuffer;
import java.util.*;
import java.util.stream.IntStream;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

class PhysicalDevice implements Comparable<PhysicalDevice> {
    VkPhysicalDevice device;
    SwapchainSupportDetails swapchainSupport;
    FeatureSupportDetails featureSupport;
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

    public void onRecreateSwapchain(long surface) {
        swapchainSupport = new SwapchainSupportDetails(device, surface);
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
                    .map(d -> collectPhysicalDeviceInfo(d, surface))
                    .filter(d -> isPhysicalDeviceSuitable(d, extensions))
                    .sorted()
                    .toArray(PhysicalDevice[]::new);
        }
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

    /** Gathers information about physical device and stores it on `PhysicalDevice` */
    private static PhysicalDevice collectPhysicalDeviceInfo(VkPhysicalDevice device, long surface) {
        PhysicalDevice physdev = new PhysicalDevice(device);

        try (MemoryStack stack = stackPush()) {
            VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.callocStack(stack);
            VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.callocStack(stack);

            vkGetPhysicalDeviceProperties(device, properties);
            vkGetPhysicalDeviceFeatures(device, features);

            physdev.deviceName = properties.deviceNameString();

            physdev.featureSupport = new FeatureSupportDetails();
            physdev.featureSupport.anisotropyEnable = features.samplerAnisotropy();
            physdev.featureSupport.maxAnisotropy = properties.limits().maxSamplerAnisotropy();

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
            physdev.swapchainSupport = new SwapchainSupportDetails(device, surface);
        }

        return physdev;
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
