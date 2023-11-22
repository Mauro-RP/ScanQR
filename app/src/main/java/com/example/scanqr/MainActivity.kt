package com.example.scanqr
//val rtspUrl = "rtsp://admin:Camara.1@192.168.100.35:554/profile1"



import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.media.MediaFormat
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.drawToBitmap
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.*
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.video.VideoFrameMetadataListener
import androidx.media3.ui.PlayerView
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
@UnstableApi


class VideoActivity : AppCompatActivity(), VideoFrameMetadataListener {
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var captureView: View

    @SuppressLint("AuthLeak")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        captureView = findViewById(R.id.captureView)

        val context = this
        val mediaSourceFactory = DefaultMediaSourceFactory(context)

        player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        playerView.player = player

        //val userAgent = "AppQRipRTSP" // Reemplaza con el agente de usuario deseado
        //val dataSourceFactory = DefaultDataSourceFactory(userAgent)
        val rtspUrl = "rtsp://admin:Camara.1@192.168.100.35:554/profile1"
        val mediaItem = MediaItem.fromUri(rtspUrl)

        //val player: Player

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        //player.addVideoFrameMetadataListener(this)
    }

    @SuppressLint("SetTextI18n")
    override fun onVideoFrameAboutToBeRendered(
        presentationTimeUs: Long,
        releaseTimeNs: Long,
        format: Format,
        mediaFormat: MediaFormat?
    ) {
        // Captura el fotograma actual y realiza la detección de códigos QR
        val bitmap = playerView.drawToBitmap()

        // Ajustar las coordenadas para capturar solo el centro del fotograma
        val screenWidth = captureView.width
        val screenHeight = captureView.height
        val x = (screenWidth).toInt() // Ajusta el valor según el ancho deseado del área central
        val y = (screenHeight).toInt() // Ajusta el valor según el alto deseado del área central
        val width = (screenWidth).toInt() // Ajusta el valor según el ancho deseado del área central
        val height = (screenHeight).toInt() // Ajusta el valor según el alto deseado del área central

        // Calcula las coordenadas y dimensiones del área de captura
        val left = x
        val top = y
        val right = x + width
        val bottom = y + height

        // Imprime las coordenadas y dimensiones del área de captura
        Log.d("CaptureArea", "Left: $left, Top: $top, Right: $right, Bottom: $bottom")
        val coordenadas = findViewById<TextView>(R.id.textView)
        coordenadas.text = "coordenadas captura: Left: $left, Top: $top, Right: $right, Bottom: $bottom"

        // Captura solo el área central del fotograma
        val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, width, height)

        // Llama a la función para detectar el código QR en el bitmap recortado
        val qrCodeText = decodeQRCode(croppedBitmap)


        // Maneja el texto del código QR detectado
        if (qrCodeText != null) {
            // Realiza alguna acción con el código QR, como mostrarlo en pantalla
            runOnUiThread {
                Log.d("QR Code", qrCodeText)
                val qrCodeTextView = findViewById<TextView>(R.id.textView)
                qrCodeTextView.text = "Texto del código QR: $qrCodeText"
                Toast.makeText(applicationContext, "Código QR: $qrCodeText", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Función para detectar y decodificar el código QR desde un Bitmap
    private fun decodeQRCode(bitmap: Bitmap): String? {
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, getNV21Data(bitmap))
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()

        return try {
            val result: Result = reader.decode(binaryBitmap)
            result.text
        } catch (e: Exception) {
            // Maneja las excepciones en caso de que no se encuentre un código QR
            null
        }
    }

    private fun getNV21Data(bitmap: Bitmap): IntArray {
        val argb8888Data = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(argb8888Data, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val yuv = IntArray(bitmap.width * bitmap.height * 3 / 2)
        encodeYUV420SP(yuv, argb8888Data, bitmap.width, bitmap.height)
        return yuv
    }

    private fun encodeYUV420SP(yuv420sp: IntArray, argb8888: IntArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize

        //var a: Int
        var r: Int
        var g: Int
        var b: Int
        var argb: Int

        for (i in 0 until height) {
            for (j in 0 until width) {
                argb = argb8888[i * width + j]
                //a = argb shr 24 and 0xff
                r = argb shr 16 and 0xff
                g = argb shr 8 and 0xff
                b = argb and 0xff

                var y = (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
                var u = (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
                var v = (112 * r - 94 * g - 18 * b + 128 shr 8) + 128

                y = if (y < 16) 16 else if (y > 255) 255 else y
                u = if (u < 0) 0 else if (u > 255) 255 else u
                v = if (v < 0) 0 else if (v > 255) 255 else v

                yuv420sp[yIndex++] = y.toByte().toInt()
                if (i % 2 == 0 && j % 2 == 0) {
                    yuv420sp[uvIndex++] = v.toByte().toInt()
                    yuv420sp[uvIndex++] = u.toByte().toInt()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

}



