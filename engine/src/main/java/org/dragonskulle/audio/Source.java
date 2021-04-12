/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * This class is used to wrap an OpenAL source and track whether it is in use or not.
 *
 * @author Harry Stoltz
 */
@Accessors(prefix = "m")
public class Source {
    @Getter @Setter private int mSource;
    @Getter @Setter private boolean mInUse = false;
}
