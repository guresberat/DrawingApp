package com.example.kidsdrawingapp

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    lateinit var ib_brush: ImageButton
    lateinit var drawing_view: DrawingView
    lateinit var ll_paint_colors: LinearLayout
    lateinit var ib_gallery: ImageButton
    lateinit var ib_undo: ImageButton
    lateinit var iv_background: ImageView
    lateinit var ib_save: ImageButton
    lateinit var fl_drawing_view_container: FrameLayout

    private var mImageButtonCurrentPaint: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawing_view = findViewById(R.id.drawing_view)
        ib_brush = findViewById(R.id.ib_brush)
        ll_paint_colors = findViewById(R.id.ll_paint_colors)
        ib_gallery = findViewById(R.id.ib_gallery)
        iv_background = findViewById(R.id.iv_background)
        ib_undo = findViewById(R.id.ib_undo)
        ib_save = findViewById(R.id.ib_save)
        fl_drawing_view_container = findViewById(R.id.fl_drawing_view_container)

        drawing_view.setSizeForBrush(10.toFloat())

        mImageButtonCurrentPaint = ll_paint_colors[1] as ImageButton

        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        ib_brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        ib_gallery.setOnClickListener {
            if (isReadStorageAllowed()) {
                val pickPhotoIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                startActivityForResult(pickPhotoIntent, GALLERY)
            } else {
                requestStoragePermission()
            }
        }
        ib_undo.setOnClickListener {
            drawing_view.onClickUndo()
        }
        ib_save.setOnClickListener{
            if(isReadStorageAllowed()){
                BitmapAsyncTask(getBitmapFromView(fl_drawing_view_container)).execute()
            }else{
                requestStoragePermission()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY) {
                try {
                    if (data!!.data != null) {
                        iv_background.visibility = View.VISIBLE
                        iv_background.setImageURI(data.data)
                    } else {
                        Toast.makeText(
                            this,
                            "Error in parsing the image or its corrupted.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")
        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawing_view.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener {
            drawing_view.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.ib_large_brush)
        largeBtn.setOnClickListener {
            drawing_view.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun paintClicked(view: View) {
        if (view !== mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton

            val colorTag = imageButton.tag.toString()

            drawing_view.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )
            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            mImageButtonCurrentPaint = view
        }

    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).toString()
            )
        ) {
            Toast.makeText(
                this,
                "You need storage permission to add a Background",
                Toast.LENGTH_SHORT
            ).show()
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            STORAGE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Permission granted,now you can read the storage file",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Oop you just denied the permission request",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)

        return returnedBitmap
    }


    private inner class BitmapAsyncTask(val mBitmap: Bitmap) : AsyncTask<Any, Void, String>() {

        override fun doInBackground(vararg params: Any?): String {
            var result = ""
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress((Bitmap.CompressFormat.PNG), 90, bytes)
                    val f =
                        File(externalCacheDir!!.absoluteFile.toString() + File.separator + "KidsDrawingApp_ " + System.currentTimeMillis() / 1000 + ".png")
                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result = f.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(result!!.isNotEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "File saved succesfully : $result",
                    Toast.LENGTH_SHORT
                ).show()
            }else{
                Toast.makeText(
                    this@MainActivity,
                    "Something went wrong while saving the file",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }


}