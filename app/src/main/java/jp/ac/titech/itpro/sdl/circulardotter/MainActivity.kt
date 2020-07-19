package jp.ac.titech.itpro.sdl.circulardotter

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.apache.commons.math3.complex.Quaternion
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : AppCompatActivity(), SensorEventListener {
    private val REQUEST_EXTERNAL_STORAGE = 120
    private val TAG = MainActivity::class.qualifiedName
    private lateinit var glView: CDGLSurfaceView

    private lateinit var manager: SensorManager
    private lateinit var gyroscope: Sensor

    private var previousTime = -1L

    private var axisX = Quaternion.I
    private var axisY = Quaternion.J
    private var axisZ = Quaternion.K

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        getPermission()

        //Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        //Remove notification bar
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_main)

        glView = findViewById(R.id.gl_view)
        glView.setActivityInfo(this, contentResolver)

        manager = getSystemService(SENSOR_SERVICE) as SensorManager
        if (manager == null) {
            Toast.makeText(this, R.string.toast_no_sensor_manager, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        gyroscope = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (gyroscope == null) {
            Toast.makeText(this, R.string.toast_no_gyroscope, Toast.LENGTH_LONG).show()
        }
    }

    private fun getPermission() {
        if (ContextCompat.checkSelfPermission(this,
                WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    WRITE_EXTERNAL_STORAGE)) {
                Log.d(TAG, "b")
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                Toast.makeText(this, "wow!", Toast.LENGTH_SHORT).show();
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                    arrayOf(WRITE_EXTERNAL_STORAGE), REQUEST_EXTERNAL_STORAGE)

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            Log.d(TAG, "permission is attached")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        glView.onResume()
        manager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        glView.onPause()
        manager.unregisterListener(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val currentTime = event.timestamp
        val nano = 0.000_000_001f
        if (previousTime > 0) {
            val dt = (currentTime - previousTime).toFloat() * nano

            val omegaXHalf = event.values[0] / 2.0f * dt  // x-axis angular velocity (rad/sec)
            val omegaYHalf = event.values[1] / 2.0f * dt  // y-axis angular velocity (rad/sec)
            val omegaZHalf = event.values[2] / 2.0f * dt  // z-axis angular velocity (rad/sec)

            // ローカル座標系における各軸上の回転を表すクォータニオン
            val qx = Quaternion(
                cos(omegaXHalf).toDouble(),
                axisX.q1 * sin(omegaXHalf),
                axisX.q2 * sin(omegaXHalf),
                axisX.q3 * sin(omegaXHalf)
            )
            val qy = Quaternion(
                cos(omegaYHalf).toDouble(),
                axisY.q1 * sin(omegaYHalf),
                axisY.q2 * sin(omegaYHalf),
                axisY.q3 * sin(omegaYHalf)
            )
            val qz = Quaternion(
                cos(omegaZHalf).toDouble(),
                axisZ.q1 * sin(omegaZHalf),
                axisZ.q2 * sin(omegaZHalf),
                axisZ.q3 * sin(omegaZHalf)
            )

            val q = Quaternion.multiply(Quaternion.multiply(qx, qy), qz)
            val qInv = q.inverse

            // ローカル座標系の更新
            axisX = Quaternion.multiply(Quaternion.multiply(q, axisX), qInv)
            axisY = Quaternion.multiply(Quaternion.multiply(q, axisY), qInv)
            axisZ = Quaternion.multiply(Quaternion.multiply(q, axisZ), qInv)

            // ローカル座標系の xy 平面上の単位円の円周上の点のうちグローバル座標系における
            // y 座標がもっとも大きいものを点 P とする時の、ローカル座標系の y 軸とベクトル
            // OP がなす角度を求める(端末の上方向と針のなす角度)
            val yVec = Vector3D(axisY.vectorPart)
            val zVec = Vector3D(axisZ.vectorPart)
            val yMax = zVec.crossProduct(Vector3D.PLUS_J).crossProduct(zVec).normalize()

            val prod = yVec.dotProduct(yMax)

            // 針が y 軸の左にあるか右にあるかによって符号を反転
            val sign = if(yVec.crossProduct(yMax).dotProduct(zVec) > 0) 1 else -1

            // 針と端末上方向がなす角度を [-PI, PI] で計算
            val inclination = sign * acos(prod)

            glView.setGlobalInfo(GlobalInfo(inclination, currentTime.toFloat() * nano))
        }
        previousTime = currentTime
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "onAccuracyChanged: accuracy=$accuracy")
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        Log.d(TAG, "request code: $requestCode")
        when (requestCode) {
            REQUEST_EXTERNAL_STORAGE -> {
                Log.d(TAG, "yeah")
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }
}

