package sg.fooyo.arcoretest

import android.content.Context
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.support.annotation.LayoutRes
import android.support.annotation.RequiresApi
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import org.jetbrains.anko.toast
import sg.fooyo.arcoretest.model.LatLng
import sg.fooyo.arcoretest.tool.ARUtil
import java.lang.Exception
import java.util.concurrent.CompletableFuture

fun Context.assignmentModel(modelName: String, func: (Renderable) -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

        ModelRenderable
            .builder()
            .setSource(this) {
                resources.assets.open(modelName)
            }
            .build()
            .thenAccept { it ->
                func(it)
            }.exceptionally {
                toast(it.message!!)
                null
            }
    }

}

fun Context.assignmentViewModel(@LayoutRes res: Int, func: (ViewRenderable) -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

        ViewRenderable.builder()
            .setView(this, res)
            .build()
            .thenAccept { renderable ->
                func(renderable)
            }
    }
}

@RequiresApi(Build.VERSION_CODES.N)
fun Context.anchorAxisToNode(anchorNode: Node) {
    fun makeLine(material: Material): ModelRenderable? {
        return ShapeFactory.makeCube(
            Vector3(.01f, .01f, 1f),
            Vector3.zero(), material
        )
    }

    val m1 = MaterialFactory.makeOpaqueWithColor(this, com.google.ar.sceneform.rendering.Color(Color.RED))
    val m2 = MaterialFactory.makeOpaqueWithColor(this, com.google.ar.sceneform.rendering.Color(Color.GREEN))
    val m3 = MaterialFactory.makeOpaqueWithColor(this, com.google.ar.sceneform.rendering.Color(Color.BLUE))
    CompletableFuture.allOf(
        m1, m2, m3
    ).handle { _, _ ->
        try {
            lineBetweenPoints(anchorNode, Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f), makeLine(m1.get()))
            lineBetweenPoints(anchorNode, Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f), makeLine(m2.get()))
            lineBetweenPoints(anchorNode, Vector3(0f, 0f, 0f), Vector3(0f, 0f, 1f), makeLine(m3.get()))
        } catch (e: Exception) {
            ARUtil.displayError(this, "unable to load render line", e.cause)
        }
    }


}

fun LatLng.toLocation(): Location {
    val l = Location("")
    l.latitude = lat
    l.longitude = lng
    return l
}