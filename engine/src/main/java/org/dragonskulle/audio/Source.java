/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public class Source {
    @Getter @Setter private int mSource;
    @Getter @Setter private boolean mInUse = false;
}
