/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_RELAXED_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Describes various possible vertical sync/presentation modes.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public enum VBlankMode {
    OFF(VK_PRESENT_MODE_IMMEDIATE_KHR, "Off"),
    SINGLE_BUFFER(VK_PRESENT_MODE_FIFO_KHR, "Single-VSync"),
    DOUBLE_BUFFER(VK_PRESENT_MODE_MAILBOX_KHR, "Double-VSync"),
    SINGLE_RELAXED(VK_PRESENT_MODE_FIFO_RELAXED_KHR, "Relaxed-VSync");

    @Getter private final int mValue;
    @Getter private final String mName;

    private VBlankMode(int value, String name) {
        mValue = value;
        mName = name;
    }

    public static VBlankMode fromInt(int val) {
        switch (val) {
            case VK_PRESENT_MODE_IMMEDIATE_KHR:
                return OFF;
            case VK_PRESENT_MODE_FIFO_KHR:
                return SINGLE_BUFFER;
            case VK_PRESENT_MODE_MAILBOX_KHR:
                return DOUBLE_BUFFER;
            case VK_PRESENT_MODE_FIFO_RELAXED_KHR:
                return SINGLE_RELAXED;
            default:
                return null;
        }
    }
}
