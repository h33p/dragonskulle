/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.dragonskulle.utils.Env.envInt;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.IntBuffer;
import java.util.logging.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

class SwapchainSupportDetails {
    VkSurfaceCapabilitiesKHR capabilities;
    VkSurfaceFormatKHR.Buffer formats;
    IntBuffer presentModes;
    public static final Logger LOGGER = Logger.getLogger("render");

    private static final int VBLANK_MODE = envInt("VBLANK_MODE", VK_PRESENT_MODE_MAILBOX_KHR);

    public SwapchainSupportDetails(VkPhysicalDevice device, long surface) {
        capabilities = VkSurfaceCapabilitiesKHR.create();
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, capabilities);

        try (MemoryStack stack = stackPush()) {
            IntBuffer length = stack.ints(0);

            vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, length, null);
            if (length.get(0) != 0) {
                formats = VkSurfaceFormatKHR.create(length.get(0));
                vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, length, formats);
            }

            vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, length, null);
            if (length.get(0) != 0) {
                presentModes = BufferUtils.createIntBuffer(length.get(0));
                vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, length, presentModes);
            }
        }
    }

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
