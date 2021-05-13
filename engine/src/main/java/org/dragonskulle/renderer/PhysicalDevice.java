/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_D24_UNORM_S8_UINT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_D32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_D32_SFLOAT_S8_UINT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_SRGB;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_TILING_LINEAR;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_TILING_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_GRAPHICS_BIT;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_16_BIT;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_2_BIT;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_32_BIT;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_4_BIT;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_64_BIT;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_8_BIT;
import static org.lwjgl.vulkan.VK10.VK_TRUE;
import static org.lwjgl.vulkan.VK10.vkEnumerateDeviceExtensionProperties;
import static org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceFeatures;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceFormatProperties;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceMemoryProperties;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceProperties;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties;

import java.nio.IntBuffer;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.utils.MathUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkFormatProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

/**
 * Describes a physical vulkan device, and the features it supports.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
@Getter
@Log
class PhysicalDevice implements Comparable<PhysicalDevice> {
    private VkPhysicalDevice mDevice;
    private SwapchainSupportDetails mSwapchainSupport;
    private FeatureSupportDetails mFeatureSupport;
    private String mDeviceName;

    private int mScore;
    private QueueFamilyIndices mIndices;

    private static final int[] DEPTH_FORMATS = {
        VK_FORMAT_D24_UNORM_S8_UINT, VK_FORMAT_D32_SFLOAT_S8_UINT, VK_FORMAT_D32_SFLOAT
    };

    /** Describes indices used for various device queues. */
    static class QueueFamilyIndices {
        Integer mGraphicsFamily;
        Integer mPresentFamily;

        /**
         * Returns whether the queue family indices complete.
         *
         * @return {@code true} if all family indices are present. {@code false} otherwise.
         */
        boolean isComplete() {
            return mGraphicsFamily != null && mPresentFamily != null;
        }

        /**
         * Get all unique family indices.
         *
         * @return array of unique family indices.
         */
        int[] uniqueFamilies() {
            return IntStream.of(mGraphicsFamily, mPresentFamily).distinct().toArray();
        }
    }

    /** Describes physical graphics feature support. */
    @Getter
    static class FeatureSupportDetails {
        /** Is anisotrophic filtering available. */
        boolean mAnisotropyEnable;
        /** Maximum anisotrophic filtering value. */
        float mMaxAnisotropy;
        /** Are geometry shaders available. */
        boolean mGeometryShaders;
        /** Optimal tiling format. */
        boolean mOptimalLinearTiling;
        /** Bitfield of supported MSAA sample values. */
        int mMsaaSamples;

        /** Required tiled formats for the device to be supported. */
        static final int[] REQUIRED_TILED_FORMATS = {VK_FORMAT_R8G8B8A8_SRGB};

        /**
         * Returns whether all required features present on the device.
         *
         * @return {@code true} if all required features are present. Currently, this is always
         *     true, but it can change in the future.
         */
        public boolean isSuitable() {
            return true;
        }
    }

    /**
     * Gathers information about physical device and stores it on `PhysicalDevice`.
     *
     * @param device input {@link VkPhysicalDevice}.
     * @param surface target surface to render to.
     */
    private PhysicalDevice(VkPhysicalDevice device, long surface) {
        this.mDevice = device;

        try (MemoryStack stack = stackPush()) {
            VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.callocStack(stack);
            VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.callocStack(stack);

            vkGetPhysicalDeviceProperties(device, properties);
            vkGetPhysicalDeviceFeatures(device, features);

            this.mDeviceName = properties.deviceNameString();

            this.mFeatureSupport = new FeatureSupportDetails();
            this.mFeatureSupport.mAnisotropyEnable = features.samplerAnisotropy();
            this.mFeatureSupport.mMaxAnisotropy = properties.limits().maxSamplerAnisotropy();
            this.mFeatureSupport.mGeometryShaders = features.geometryShader();
            this.mFeatureSupport.mOptimalLinearTiling =
                    findSupportedFormat(
                                    FeatureSupportDetails.REQUIRED_TILED_FORMATS,
                                    VK_IMAGE_TILING_OPTIMAL,
                                    VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT)
                            != -1;
            this.mFeatureSupport.mMsaaSamples =
                    properties.limits().framebufferColorSampleCounts()
                            & properties.limits().framebufferDepthSampleCounts();

            this.mScore = 0;

            // Prioritize dedicated graphics cards
            if (properties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
                this.mScore += 5000;
            }

            // Check maximum texture sizes, prioritize largest
            this.mScore += properties.limits().maxImageDimension2D();

            QueueFamilyIndices indices = new QueueFamilyIndices();

            VkQueueFamilyProperties.Buffer buf = getQueueFamilyProperties(device);

            IntBuffer presentSupport = stack.ints(0);

            int capacity = buf.capacity();

            for (int i = 0; i < capacity && !indices.isComplete(); i++) {
                int queueFlags = buf.get(i).queueFlags();

                if ((queueFlags & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    indices.mGraphicsFamily = i;
                }

                vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, presentSupport);

                if (presentSupport.get(0) == VK_TRUE) {
                    indices.mPresentFamily = i;
                }
            }

            this.mIndices = indices;
            this.mSwapchainSupport = new SwapchainSupportDetails(device, surface);
        }
    }

    /**
     * Order physical devices by their suitability score.
     *
     * @param other other physical device to compare against.
     * @return ordering of the devices.
     */
    @Override
    public int compareTo(PhysicalDevice other) {
        return Integer.compare(other.mScore, mScore);
    }

    /**
     * Find supported depth texture format.
     *
     * @return supported format for depth texture
     */
    public int findDepthFormat() {
        return findSupportedFormat(
                DEPTH_FORMATS,
                VK_IMAGE_TILING_OPTIMAL,
                VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT);
    }

    /**
     * Finds the highest suitable MSAA sample count.
     *
     * @param sampleCount wanted sample count
     * @return highest supported sample count that is no larger than {@code sampleCount}
     */
    public int findSuitableMSAACount(int sampleCount) {
        sampleCount = MathUtils.roundDownToPow2(sampleCount);
        if (sampleCount < 1) {
            sampleCount = 1;
        }

        if (sampleCount >= 64 && (mFeatureSupport.mMsaaSamples & VK_SAMPLE_COUNT_64_BIT) != 0) {
            return VK_SAMPLE_COUNT_64_BIT;
        } else if (sampleCount >= 32
                && (mFeatureSupport.mMsaaSamples & VK_SAMPLE_COUNT_32_BIT) != 0) {
            return VK_SAMPLE_COUNT_32_BIT;
        } else if (sampleCount >= 16
                && (mFeatureSupport.mMsaaSamples & VK_SAMPLE_COUNT_16_BIT) != 0) {
            return VK_SAMPLE_COUNT_16_BIT;
        } else if (sampleCount >= 8
                && (mFeatureSupport.mMsaaSamples & VK_SAMPLE_COUNT_8_BIT) != 0) {
            return VK_SAMPLE_COUNT_8_BIT;
        } else if (sampleCount >= 4
                && (mFeatureSupport.mMsaaSamples & VK_SAMPLE_COUNT_4_BIT) != 0) {
            return VK_SAMPLE_COUNT_4_BIT;
        } else if (sampleCount >= 2
                && (mFeatureSupport.mMsaaSamples & VK_SAMPLE_COUNT_2_BIT) != 0) {
            return VK_SAMPLE_COUNT_2_BIT;
        }

        return VK_SAMPLE_COUNT_1_BIT;
    }

    /**
     * Find supported device format.
     *
     * @param candidates list of formats to try.
     * @param tiling target tiling mode.
     * @param features required tiling features.
     * @return supported format. {@code -1} if no format available.
     */
    private int findSupportedFormat(int[] candidates, int tiling, int features) {
        try (MemoryStack stack = stackPush()) {
            VkFormatProperties props = VkFormatProperties.callocStack(stack);
            for (int format : candidates) {
                vkGetPhysicalDeviceFormatProperties(mDevice, format, props);
                if (tiling == VK_IMAGE_TILING_LINEAR
                        && (props.linearTilingFeatures() & features) == features) {
                    return format;
                } else if (tiling == VK_IMAGE_TILING_OPTIMAL
                        && (props.optimalTilingFeatures() & features) == features) {
                    return format;
                }
            }

            return -1;
        }
    }

    /**
     * Update swapchain support details.
     *
     * @param surface new surface.
     */
    public void onRecreateSwapchain(long surface) {
        mSwapchainSupport = new SwapchainSupportDetails(mDevice, surface);
    }

    /**
     * Find a suitable memory type for the GPU.
     *
     * @param filterBits memory types to use.
     * @param properties required memory properties.
     * @return supported memory type.
     * @throws RendererException if unable to find suitable memory type.
     */
    public int findMemoryType(int filterBits, int properties) throws RendererException {
        try (MemoryStack stack = stackPush()) {
            VkPhysicalDeviceMemoryProperties memProperties =
                    VkPhysicalDeviceMemoryProperties.callocStack(stack);
            vkGetPhysicalDeviceMemoryProperties(mDevice, memProperties);

            for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
                if ((filterBits & (1 << i)) != 0
                        && (memProperties.memoryTypes(i).propertyFlags() & properties)
                                == properties) {
                    return i;
                }
            }

            throw new RendererException("Failed to find suitable memory type!");
        }
    }

    /**
     * Picks a physical device with required features.
     *
     * @param instance target vulkan instance.
     * @param surface target surface.
     * @param targetDevice target device name to pick. Use {@code null}, or empty string to pick the
     *     first (most suitable) device in the list.
     * @param neededExtensions needed device extensions for the target device.
     * @return chosen physical device. {@code null} if could not find a suitable one.
     */
    public static PhysicalDevice pickPhysicalDevice(
            VkInstance instance, long surface, String targetDevice, Set<String> neededExtensions) {
        PhysicalDevice[] devices = enumeratePhysicalDevices(instance, surface, neededExtensions);

        if (devices.length == 0) {
            return null;
        } else if (targetDevice == null) {
            return devices[0];
        }

        for (PhysicalDevice d : devices) {
            if (d.mDeviceName.contains(targetDevice)) {
                return d;
            }
        }

        log.severe("Failed to find suitable physical device!");
        log.info("Valid devices:");
        for (PhysicalDevice d : devices) {
            log.info(d.mDeviceName);
        }

        return null;
    }

    /**
     * Collect all compatible physical GPUs into an array, sorted by decreasing score.
     *
     * @param instance current vulkan instance.
     * @param surface target surface.
     * @param extensions required extensions by the device.
     * @return Sorted array of physical GPUs.
     */
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

    /**
     * Get device extension properties.
     *
     * @param stack stack on which the buffer will be allocated on.
     * @return vulkan extension properties buffer containing one entry, and properties within.
     */
    private VkExtensionProperties.Buffer getDeviceExtensionProperties(MemoryStack stack) {
        IntBuffer propertyCount = stack.ints(0);
        vkEnumerateDeviceExtensionProperties(mDevice, (String) null, propertyCount, null);
        VkExtensionProperties.Buffer properties =
                VkExtensionProperties.mallocStack(propertyCount.get(0), stack);
        vkEnumerateDeviceExtensionProperties(mDevice, (String) null, propertyCount, properties);
        return properties;
    }

    /**
     * Check whether the device in question is suitable for us.
     *
     * @param device target physical device.
     * @param extensions required physical device extensions.
     * @return {@code true}, if the device is suitable, {@code false} otherwise.
     */
    private static boolean isPhysicalDeviceSuitable(PhysicalDevice device, Set<String> extensions) {
        try (MemoryStack stack = stackPush()) {
            return device.mIndices.isComplete()
                    && device.mFeatureSupport.isSuitable()
                    && device.getDeviceExtensionProperties(stack).stream()
                            .map(VkExtensionProperties::extensionNameString)
                            .collect(toSet())
                            .containsAll(extensions)
                    && device.mSwapchainSupport.isAdequate();
        }
    }

    /**
     * Utility for retrieving VkQueueFamilyProperties list.
     *
     * @param device target physical device
     * @return list of queue family properties.
     */
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
