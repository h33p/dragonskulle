/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_B8G8R8A8_SRGB;

import java.nio.IntBuffer;
import lombok.extern.java.Log;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

/**
 * This provides details about swapchain support for particular physical device.
 *
 * @author Aurimas Blažulionis
 */
@Log
class SwapchainSupportDetails {
    VkSurfaceCapabilitiesKHR mCapabilities;
    VkSurfaceFormatKHR.Buffer mFormats;
    VBlankMode[] mVBlankModes;

    /**
     * Constructor for {@link SwapchainSupportDetails}.
     *
     * @param device physical device handle
     * @param surface surface that we want to display on
     */
    public SwapchainSupportDetails(VkPhysicalDevice device, long surface) {
        mCapabilities = VkSurfaceCapabilitiesKHR.create();
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, mCapabilities);

        try (MemoryStack stack = stackPush()) {
            IntBuffer length = stack.ints(0);

            vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, length, null);
            if (length.get(0) != 0) {
                mFormats = VkSurfaceFormatKHR.create(length.get(0));
                vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, length, mFormats);
            }

            vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, length, null);
            if (length.get(0) != 0) {
                IntBuffer presentNodes = stack.callocInt(length.get(0));
                vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, length, presentNodes);

                mVBlankModes = new VBlankMode[length.get(0)];

                for (int i = 0; i < length.get(0); i++) {
                    mVBlankModes[i] = VBlankMode.fromInt(presentNodes.get(i));
                }
            }
        }
    }

    /**
     * Check if the swapchain is supported for this device.
     *
     * @return {@code true} if possible to use this device for presentation, {@code false}
     *     otherwise.
     */
    public boolean isAdequate() {
        return mFormats != null && mVBlankModes != null && mVBlankModes.length > 0;
    }

    /**
     * Choose a compatible surface format, prioritizing sRGB.
     *
     * @return first compatible surface format.
     */
    public VkSurfaceFormatKHR chooseSurfaceFormat() {
        VkSurfaceFormatKHR format =
                mFormats.stream()
                        .filter(
                                f ->
                                        f.format() == VK_FORMAT_B8G8R8A8_SRGB
                                                && f.colorSpace()
                                                        == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                        .findAny()
                        .orElseGet(() -> mFormats.get(0));
        log.fine(
                String.format(
                        "Picked surface format: %d %d", format.format(), format.colorSpace()));
        return format;
    }

    /**
     * Choose a compatible presentation mode, prioritizing Triple Buffered VSync.
     *
     * @param preferredMode preferred vblank mode.
     * @return compatible presentation mode
     */
    public VBlankMode choosePresentMode(VBlankMode preferredMode) {
        // VSync - guaranteed to be available
        VBlankMode presentMode = VBlankMode.SINGLE_BUFFER;

        // Prioritized tripple buffered VSync, or use no vsync
        // We need configuration for this.
        for (VBlankMode mode : mVBlankModes) {
            if (mode == preferredMode) {
                return preferredMode;
            }

            if (mode == VBlankMode.DOUBLE_BUFFER || mode == VBlankMode.OFF) {
                presentMode = mode;
            }
        }

        return presentMode;
    }

    /**
     * Choose a compatible resolution, targetting current window resolution.
     *
     * @param window handle to the window
     * @return window extent bounds
     */
    public VkExtent2D chooseExtent(long window) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer x = stack.ints(0);
            IntBuffer y = stack.ints(0);
            glfwGetFramebufferSize(window, x, y);

            log.finer(String.format("Extent TRY: %dx%d", x.get(0), y.get(0)));
            log.finer(
                    String.format(
                            "MAX: %dx%d",
                            mCapabilities.maxImageExtent().width(),
                            mCapabilities.maxImageExtent().height()));
            log.finer(
                    String.format(
                            "MIN: %dx%d",
                            mCapabilities.minImageExtent().width(),
                            mCapabilities.minImageExtent().height()));

            VkExtent2D extent = VkExtent2D.create();
            extent.set(
                    Integer.max(
                            Integer.min(x.get(0), mCapabilities.maxImageExtent().width()),
                            mCapabilities.minImageExtent().width()),
                    Integer.max(
                            Integer.min(y.get(0), mCapabilities.maxImageExtent().height()),
                            mCapabilities.minImageExtent().height()));

            log.finer(String.format("Extent: %dx%d", extent.width(), extent.height()));

            return extent;
        }
    }

    /**
     * Choose the target number of swapchain images used.
     *
     * @return optimal target number of swapchain images.
     */
    public int chooseImageCount() {
        int imageCount = mCapabilities.minImageCount() + 1;
        if (mCapabilities.maxImageCount() > 0 && imageCount > mCapabilities.maxImageCount()) {
            return mCapabilities.maxImageCount();
        } else {
            return imageCount;
        }
    }
}
