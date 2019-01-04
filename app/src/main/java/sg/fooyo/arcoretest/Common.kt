package sg.fooyo.arcoretest

import com.google.ar.core.Pose
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import java.lang.Math.*

fun getAngle(latSource: Double, lngSource: Double, latDestination: Double, lngDestination: Double): Double {
    val lat1: Double = latSource / 180 * Math.PI
    val lng1: Double = lngSource / 180 * Math.PI
    val lat2: Double = latDestination / 180 * Math.PI
    val lng2: Double = lngDestination / 180 * Math.PI

    val y = sin(lng2 - lng1) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(lng2 - lng1)

    val tan2 = atan2(y, x)
    val degre = tan2 * 180 / Math.PI
    return if (degre < 0) {
        degre + 360
    } else {
        degre
    }
}


fun lineBetweenPoints(anchor: Node, point1: Vector3, point2: Vector3, render: ModelRenderable?) {
    val lineNode = Node()

    /* First, find the vector extending between the two points and define a look rotation in terms of this
    Vector. */

    val difference = Vector3.subtract(point1, point2)
    val directionFromTopToBottom = difference.normalized()
    val rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up())

    /* Then, create a rectangular prism, using ShapeFactory.makeCube() and use the difference vector
     to extend to the necessary length.  */

    /* Last, set the local rotation of the node to the rotation calculated earlier and set the local position to
        the midpoint between the given points . */

    lineNode.setParent(anchor)
    lineNode.renderable = render
    lineNode.localPosition = Vector3.add(point1, point2).scaled(.5f)
    lineNode.localRotation = rotationFromAToB

}

fun axisRotation(axis: Int, angleRad: Float): Pose {
    val sinHalf = Math.sin((angleRad / 2).toDouble()).toFloat()
    val cosHalf = Math.cos((angleRad / 2).toDouble()).toFloat()

    return when (axis) {
        0 -> Pose.makeRotation(sinHalf, 0f, 0f, cosHalf)
        1 -> Pose.makeRotation(0f, sinHalf, 0f, cosHalf)
        2 -> Pose.makeRotation(0f, 0f, sinHalf, cosHalf)
        else -> throw IllegalArgumentException("invalid axis $axis")
    }
}