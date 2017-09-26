package betrip.kitttn.photochooser

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView

/**
 * @author kitttn
 */

class TestActivity : AppCompatActivity() {
    val chooser = MediaChooserFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        val openGalleryBtn = findViewById<Button>(R.id.openGalleryBtn)
        openGalleryBtn.setOnClickListener { _ -> chooser.openGallery() }

        val openCameraBtn = findViewById<Button>(R.id.openCameraBtn)
        openCameraBtn.setOnClickListener { _ -> chooser.openCamera() }

        val image = findViewById<ImageView>(R.id.imageView)

        supportFragmentManager
                .beginTransaction()
                .add(chooser, "chooser")
                .commit()

        chooser.onBitmapLoaded { image.setImageBitmap(it) }
    }
}