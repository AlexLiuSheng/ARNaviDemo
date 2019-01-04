package sg.fooyo.arcoretest.vm

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.google.ar.sceneform.rendering.ModelRenderable

class RenderVM : ViewModel() {
    private lateinit var m: MutableLiveData<ModelRenderable>
    fun init(src: String) {

        m = MutableLiveData()

    }

    fun getRenderable(): MutableLiveData<ModelRenderable> {
        return m;
    }
}