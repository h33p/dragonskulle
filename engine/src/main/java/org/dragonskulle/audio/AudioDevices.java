/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALUtil;

/**
 * Utility class for enumerating OpenAL devices
 *
 * @author Harry Stoltz
 */
public class AudioDevices {

    /**
     * Get the name of the default device
     *
     * @return Name of the default device
     */
    public static String getDefaultDeviceName() {
        return ALC11.alcGetString(0L, ALC11.ALC_DEFAULT_DEVICE_SPECIFIER);
    }

    /**
     * Get a list of all available OpenAL devices. If we are unable to enumerate devices, a list
     * containing the default device is returned
     *
     * @return A list of device names
     */
    public static List<String> getAvailableDevices() {
        if (ALC11.alcIsExtensionPresent(0L, "ALC_ENUMERATION_EXT")) {
            return ALUtil.getStringList(0L, ALC11.ALC_DEVICE_SPECIFIER);
        } else {
            // If we cant enum the devices just get the default device and return that
            String defaultDevice = ALC11.alcGetString(0L, ALC11.ALC_DEFAULT_DEVICE_SPECIFIER);
            ArrayList<String> ret = new ArrayList<>();
            ret.add(defaultDevice);
            return ret;
        }
    }
}
