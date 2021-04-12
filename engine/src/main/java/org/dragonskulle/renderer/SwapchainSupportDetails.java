/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.dragonskulle.utils.Env.envInt;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_B8G8R8A8_SRGB;

import java.nio.IntBuffer;
import lombok.extern.java.Log;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

@Log
class SwapchainSupportDetails {
    VkSurfaceCapabilitiesKHR mCapabilities;
    VkSurfaceFormatKHR.Buffer mFormats;
    IntBuffer mPresentNodes;

    private static final int VBLANK_MODE = envInt("VBLANK_MODE", VK_PRESENT_MODE_FIFO_KHR);

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
                mPresentNodes = BufferUtils.createIntBuffer(length.get(0));
                vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, length, mPresentNodes);
            }
        }
    }

    public boolean isAdequate() {
        return mFormats != null && mPresentNodes != null;
    }

    /** Choose a compatible surface format, prioritizing SRGB. */
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

    /** Choose a compatible presentation mode, prioritizing Triple Buffered VSync. */
    public int choosePresentMode() {
        // VSync - guaranteed to be available
        int presentMode = VK_PRESENT_MODE_FIFO_KHR;

        // Prioritized tripple buffered VSync, or use no vsync
        // We need configuration for this.
        for (int i = 0; presentMode != VBLANK_MODE && i < mPresentNodes.capacity(); i++) {
            int pm = mPresentNodes.get(i);
            if (pm == VBLANK_MODE
                    || pm == VK_PRESENT_MODE_MAILBOX_KHR
                    || pm == VK_PRESENT_MODE_IMMEDIATE_KHR) {
                presentMode = pm;
            }
        }

        return presentMode;
    }

    /** Choose a compatible resolution, targetting current window resolution. */
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

    public int chooseImageCount() {
        int imageCount = mCapabilities.minImageCount() + 1;
        if (mCapabilities.maxImageCount() > 0 && imageCount > mCapabilities.maxImageCount()) {
            return mCapabilities.maxImageCount();
        } else {
            return imageCount;
        }
    }
}
