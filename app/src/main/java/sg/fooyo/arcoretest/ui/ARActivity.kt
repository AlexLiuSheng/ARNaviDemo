package sg.fooyo.arcoretest.ui

import android.location.Location
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.GestureDetector
import android.view.View
import android.widget.TextView
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.PlaneRenderer
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.rendering.ViewRenderable
import kotlinx.android.synthetic.main.activity_base_ar.*
import org.jetbrains.anko.alert
import sg.fooyo.arcoretest.R
import sg.fooyo.arcoretest.assignmentModel
import sg.fooyo.arcoretest.assignmentViewModel
import sg.fooyo.arcoretest.model.LatLng
import sg.fooyo.arcoretest.toLocation
import sg.fooyo.arcoretest.tool.CompassHelper


@RequiresApi(Build.VERSION_CODES.N)
class ARActivity : BaseARActivity(), Scene.OnUpdateListener {
    private var stickRenderable: ModelRenderable? = null
    private var boardRenderable: ModelRenderable? = null
    private lateinit var gestureDetector: GestureDetector
    private var viewR: ViewRenderable? = null

//    private var lineX: ModelRenderable? = null
//    private var lineY: ModelRenderable? = null
//    private var lineZ: ModelRenderable? = null


    private var hasPlacedObject = false
    private var hasFinishedLoading = false;


    private val TAG = "HEHE"
    private lateinit var compassHelper: CompassHelper

    private val locationArr = ArrayList<LatLng>()
    private var time = 0
    private var hasGotLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//29.557539&lon=106.577057
        //104.065692,30.581406
//        currentLocation.latitude = 29.557539
//        currentLocation.longitude = 106.577057
        //104.065113,30.583683
        //104.153661,30.681403
        init()
        initModel()
//        initPlaneRender()


    }

    override fun onResume() {
        super.onResume()
        compassHelper.onResume()

    }

    override fun onLocationGot(location: Location?) {
        super.onLocationGot(location)
        currentLocation?.let {
            if (!hasGotLocation) {
                //104.040528,30.667087
                locationArr.add(LatLng(30.667087, 104.040528, "四川博物馆"))
                 //104.147618,30.635065
                locationArr.add(LatLng(30.635065, 104.147618, "成都东站"))
                //locationArr.add(LatLng(it.latitude, it.longitude, "当前位置"))
                locationArr.add(LatLng(30.681403, 104.153661, "成都理工大学"))
                //104.070742,30.574935
                locationArr.add(LatLng(30.574935, 104.070742, "环球中心"))

                hasGotLocation = true
            }
        }

    }

    private fun init() {
        compassHelper = CompassHelper(this)
        compassHelper.setCallBack {
            tvNum.text = it.toString()

        }



        arSceneView?.apply {
            //            scene.setOnTouchListener { _, _ -> false }
            scene
                .setOnTouchListener { _, _ ->
                    // If the solar system hasn't been placed yet, detect a tap and then check to see if
                    // the tap occurred on an ARCore plane to place the solar system.

//                    if (!hasPlacedObject) {
//
//                        return@setOnTouchListener gestureDetector.onTouchEvent(event)
//                    }
                    // Otherwise return false so that the touch event can propagate to the scene.

                    return@setOnTouchListener false
                }
            scene.addOnUpdateListener(this@ARActivity)
        }


    }


    private fun renderModel(anchor: Anchor) {

        arSceneView?.also {

            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(it.scene)


//            anchorAxisToNode(anchorNode)

//
//            anchor.pose.compose(compassHelper.rotateZToNorthPose())
            // Create the transformable andy and add it to the anchor.

            val parent = Node()
            parent.setParent(anchorNode)


//            parent.localRotation=Quaternion.axisAngle(Vector3(1f, 0f, 0f), -90f)
            val stick = Node()
            stick.setParent(parent)
            stick.renderable = stickRenderable

            //设置与地面距离为0 绕父节点旋转，旋转中心是自身中心，有一部分会在地下
            stick.localPosition = Vector3(0f, 0.98f, 0f)

            stick.localRotation = Quaternion.axisAngle(Vector3(1f, 0f, 0f), -90f)

            time = 0
            for (latLng in locationArr) {
                addBoardModel(stick, latLng)

            }

        }


    }


    override fun onPause() {
        super.onPause()
        compassHelper.onPause()

    }

    /**
     * 父节点旋转之后，对应的坐标系也跟着旋转，右手定责
     */
    private fun addBoardModel(stick: Node, latLng: LatLng) {
        fun initBoard(panel: Node): Node {
            panel.localPosition = Vector3(0.03f, 0.03f, 0.7f)
            //init board
            val board = Node()
            board.setParent(panel)
            board.renderable = boardRenderable
            Quaternion.identity()
            val p = Quaternion.axisAngle(Vector3(1f, 0f, 0f), 90f)
            val q = Quaternion.axisAngle(Vector3(0f, 1f, 0f), 90f)
            board.localRotation = Quaternion.multiply(p, q)
            val step = 0.3f
            board.localPosition = Vector3(0.15f, 0f, -step * time)
            board.setOnTapListener { hitTestResult, motionEvent ->
                alert {
                    title = "Tip"
                    message = "点击"
                    positiveButton("ok") { }
                    show()
                }
            }
            return board
        }
        Quaternion.identity()
        val rotationQuaternion = Quaternion.axisAngle(Vector3(0f, 0f, 1f), -90f)
        val panel = Node()
        panel.setParent(stick)

        val angle = currentLocation!!.bearingTo(latLng.toLocation())
        Log.e(TAG, "angle1:$angle\n")
        panel.localRotation =
                Quaternion.multiply(rotationQuaternion, Quaternion.axisAngle(Vector3(0f, 0f, 1f), angle))
        //binding text to board
        initTextModel(initBoard(panel), latLng.name)
        time++


    }

    private fun initTextModel(parent: Node, str: String) {

        assignmentViewModel(R.layout.text) {
            for (i in 0..1) {
                viewR = it
                setText(viewR, str)
                val text = Node()
                text.setParent(parent)
                text.renderable = viewR
                Quaternion.identity()
                val q1 = Quaternion.axisAngle(Vector3(0f, 1f, 0f), 90f)
                text.localRotation = q1
                if (i == 0)
                    text.localPosition = Vector3(-0.01f, 0f, -0f)
                else
                    text.localPosition = Vector3(0.01f, 0f, -0f)

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


    }

    override fun onUpdate(frameTime: FrameTime?) {

        arSceneView?.let {
            val frame = it.arFrame
            //设置半径
            initPlaneRender()
            compassHelper.onUpdate(it.session.update())
            if (!hasLocationPermission) {
                requestLocationPermission()
            }

            if (frame != null) {
                if (!hasPlacedObject && renderModel(screenCenter().x, screenCenter().y, frame)) {
                    hasPlacedObject = true

                }
            }
        }
    }

    private fun initPlaneRender() {
        val sampler = Texture.Sampler.builder()
            .setMinFilter(Texture.Sampler.MinFilter.LINEAR)
            .setMagFilter(Texture.Sampler.MagFilter.LINEAR)
            .setWrapMode(Texture.Sampler.WrapMode.REPEAT).build()
        // Build texture with sampler
        val trigrid = Texture.builder()
            .setSource(this, R.drawable.trigrid2)
            .setSampler(sampler).build()
        arSceneView?.also {
            it.planeRenderer.material.thenAcceptBoth(
                trigrid
            ) { material, texture ->

                material.setTexture(PlaneRenderer.MATERIAL_TEXTURE, texture)
//                material.setFloat(PlaneRenderer.MATERIAL_SPOTLIGHT_RADIUS, 100f)
//                material.setFloat3(PlaneRenderer.MATERIAL_COLOR, Color(android.graphics.Color.WHITE))
//                material.setFloat2(PlaneRenderer.MATERIAL_UV_SCALE, 1f, 1.19245f)
////                material.setFloat2(PlaneRenderer.MATERIAL_UV_SCALE, 50f, 50f);
            }
        }
    }

    private var anchor: Anchor? = null
    private var originPose: Pose? = null
    private fun renderModel(x: Float, y: Float, frame: Frame?): Boolean {

        if (frame != null && frame.camera.trackingState == TrackingState.TRACKING) {


            //模拟frame点击
            for (hit in frame.hitTest(x, y)) {
                //获取点击的track对象
                val trackable = hit.trackable
                //判断track对象是否是Plane并且pose是否是
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose) && hasGotLocation) {
//                    val anchor = hit.createAnchor()
                    if (anchor != null) {
                        anchor!!.detach()
                    }
                    if (originPose == null)
                        originPose = hit.hitPose
                    val pose = originPose!!.compose(compassHelper.rotateXToEastPose())
                    anchor = arSceneView?.session!!.createAnchor(pose)
                    renderModel(anchor!!)
                    return true
                }
            }
        }
        return false
    }


    //A method to find the screen center. This is used while placing objects in the scene
    private fun screenCenter(): Vector3 {
        val vw = findViewById<View>(android.R.id.content)
        return Vector3(vw.width / 2f, vw.height / 2f, 0f)
    }

    private fun setText(render: ViewRenderable?, str: String) {
        render?.run {
            view.findViewById<TextView>(R.id.text)?.text = str
        }

    }

}