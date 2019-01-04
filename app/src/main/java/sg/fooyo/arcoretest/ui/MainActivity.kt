package sg.fooyo.arcoretest.ui

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import org.jetbrains.anko.toast
import sg.fooyo.arcoretest.R
import sg.fooyo.arcoretest.assignmentModel
import sg.fooyo.arcoretest.assignmentViewModel
import sg.fooyo.arcoretest.tool.ARUtil


class MainActivity : AppCompatActivity(), Scene.OnUpdateListener {



    private var arFragment: ArFragment? = null
    private var stickRenderable: ModelRenderable? = null
    private var boardRenderable: ModelRenderable? = null

    private var viewR: ViewRenderable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        maybeEnableArButton()
        if (!ARUtil.checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?
        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().

//        arFragment?.let {
//            it.arSceneView.scene.addOnUpdateListener(this)
//
//        }
        initModel()
//
        arFragment?.let {
            it.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, motionEvent: MotionEvent ->
                if (stickRenderable == null) {
                    toast("return")
                    return@setOnTapArPlaneListener
                }
                // Create the Anchor.
                val anchor = hitResult.createAnchor()
                renderModel(anchor)
            }
        }


    }


    private fun initModel() {
        assignmentModel("stick.sfb") {
            stickRenderable = it as ModelRenderable
        }
        assignmentModel("uk.sfb") {
            boardRenderable = it as ModelRenderable
        }
        assignmentViewModel(R.layout.text) { t ->
            viewR = t
            viewR?.let {
                //                        it.horizontalAlignment = ViewRenderable.HorizontalAlignment.LEFT
                it.verticalAlignment = ViewRenderable.VerticalAlignment.BOTTOM
            }
        }


    }

    override fun onUpdate(frameTime: FrameTime?) {
        arFragment?.let {

            val frame = it.arSceneView.arFrame
            if (frame != null) {
                //get the trackables to ensure planes are detected
                val trackables = frame.getUpdatedTrackables(Plane::class.java).iterator()
                while (trackables.hasNext()) {


                    val plane = trackables.next() as Plane
                    if (plane.trackingState == TrackingState.TRACKING) {
                        //Hide the plane discovery helper animation
                        it.planeDiscoveryController.hide()
                        //Get all added anchors to the frame
                        val iterableAnchor = frame.updatedAnchors.iterator()
                        //place the first object only if no previous anchors were added
                        //如果还没有锚点，才创建锚点
                        if (!iterableAnchor.hasNext()) {

                            //Perform a hit test at the center of the screen to place an object without tapping
                            val hitTest = frame.hitTest(frame.screenCenter().x, frame.screenCenter().y)
                            //iterate through all hits
                            val hitTestIterator = hitTest.iterator()
                            while (hitTestIterator.hasNext()) {
                                val hitResult = hitTestIterator.next()
                                //Create an anchor at the plane hit
                                val modelAnchor = plane.createAnchor(hitResult.hitPose)
                                renderModel(modelAnchor)

                            }


                        }


                    }
                }

            }
        }
    }


    private fun renderModel(anchor: Anchor) {

        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arFragment!!.arSceneView.scene)
//


        // Create the transformable andy and add it to the anchor.
        val parent=TransformableNode(arFragment!!.transformationSystem)
        parent.setParent(anchorNode)
        parent.select()


        val stick = Node()
        stick.setParent(parent)
        stick.renderable = stickRenderable
        stick.worldRotation = Quaternion.axisAngle(Vector3(1f, 0f, 0f), -90f)


        for (i in 0..3) {

            addBoardModel(stick, i)
        }


    }

    private fun addBoardModel(stick: Node, i: Int) {
        val board = Node()
        board.setParent(stick)
        board.renderable = boardRenderable
        Quaternion.identity()
        val q: Quaternion = if (i == 1)
            Quaternion.axisAngle(Vector3(0f, 1f, 0f), -135f)
        else
            Quaternion.axisAngle(Vector3(0f, 1f, 0f), -90f)

        val p = Quaternion.axisAngle(Vector3(1f, 0f, 0f), -90f)
        val quaternion = Quaternion.multiply( p,q)

        board.localRotation =quaternion
//        board.worldRotation=quaternion

        val initY = 1.47f
//        val pos: Vector3
        val pos = if (i == 1)
            Vector3(0.17f, 0.01f, initY + i * -0.3f)
        else
            Vector3(-0.1f, 0.01f, initY + i * -0.3f)

        board.localPosition = pos


//        val textNode = Node()
//        textNode.renderable = viewR
//        textNode.localRotation = Quaternion.axisAngle(Vector3(1f, 0f, 0f), 90f)
//        textNode.localScale = Vector3(1f, 1f, 1f)
//        val pos2= Vector3(-0.1f, -0.03f, initY + i * -0.3f)
//        textNode.localPosition = pos2
//        stick.addChild(textNode)

    }


    //A method to find the screen center. This is used while placing objects in the scene
    private fun Frame.screenCenter(): Vector3 {
        val vw = findViewById<View>(android.R.id.content)
        return Vector3(vw.width / 2f, vw.height / 2f, 0f)
    }

    fun maybeEnableArButton() {
        // Likely called from Activity.onCreate() of an activity with AR buttons.
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            // re-query at 5Hz while we check compatibility.
            Handler().postDelayed(Runnable { maybeEnableArButton() }, 200)
        }
        if (availability.isSupported) {
            toast("支持arcore")
            // indicator on the button.
        } else { // unsupported or unknown
            toast("不支持arcore")
        }
    }



}
