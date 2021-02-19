/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import org.dragonskulle.core.Matrix4;

import java.io.Serializable;

/**
 * Base Transform class
 *
 * @author Harry Stoltz
 *     <p>All GameObjects will have a Transform object which stores the position, rotation and scale
 *     of the object (As right, up, forward and position in a 4x4 Matrix) To actually use these
 *     positions the Transform must be cast to either HexTransform or 3DTransform to extract the
 *     information in the correct coordinate system
 */
public class Transform extends Component implements Serializable {

    Matrix4 localMatrix;

    // Maybe cache the worldMatrix and only update when the parent's transform has been updated?

    /** */
    public Transform() {}

    // TODO: Transform class

    @Override
    protected void onDestroy() {

        // TODO: Destroy for transform

    }
}
