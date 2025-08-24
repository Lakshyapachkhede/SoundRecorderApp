package com.pachkhede.recordingaudio

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioRecordingMonitor
import android.media.MediaRecorder
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresPermission
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder


class MainActivity : AppCompatActivity() {
    private var audioRecord : AudioRecord? = null
    private lateinit var audioWaveView : AudioWave
    private lateinit var recordButton : ImageButton
    private lateinit var timerText : TextView
    private var isRecording = false
    private lateinit var outputFileDescriptor : FileDescriptor
    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var  outputStream : OutputStream
    private var recordingThread: Thread? = null
    private lateinit var outputFile: File
    private val updateTimer = object : Runnable {
        override fun run() {
            val minutes = seconds / 60
            val secs = seconds % 60
            timerText.text = String.format("%02d:%02d", minutes, secs)
            seconds++
            handler.postDelayed(this, 1000)
        }
    }




    private val requestMicrophonePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if(isGranted){
            toggleRecording()
        } else{
            Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        recordButton = findViewById(R.id.recordButton)
        timerText = findViewById(R.id.timerText)
        audioWaveView = findViewById(R.id.audioWaveView)

        recordButton.setOnClickListener {
            if (!isRecording) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    toggleRecording()
                } else {
                    requestMicrophonePermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            } else {
                toggleRecording()
            }
        }


    }

    private fun toggleRecording(){
        if(isRecording){
            stopRecording()
        } else {
            startRecording()
        }
    }



//    private fun startRecording(){
//
//        val sampleRate = 44100
//        val bufferSize = AudioRecord.getMinBufferSize(
//            sampleRate,
//            AudioFormat.CHANNEL_IN_MONO,
//            AudioFormat.ENCODING_PCM_16BIT
//        )
//
//        audioRecord = AudioRecord(
//            MediaRecorder.AudioSource.MIC,
//            sampleRate,
//            AudioFormat.CHANNEL_IN_MONO,
//            AudioFormat.ENCODING_PCM_16BIT,
//            bufferSize
//        )
//
//        isRecording = true
//        audioRecord?.startRecording()
//
//        recordButton.setImageResource(R.drawable.pause_icon)
//        seconds = 0
//        handler.post(updateTimer)
//
//        // Background thread for reading audio
//        Thread {
//            val buffer = ShortArray(bufferSize)
//            while (isRecording) {
//                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
//                if (read > 0) {
//                    val data = buffer.copyOf(read) // cut to actual read size
//                    runOnUiThread {
//                        audioWaveView.updateAudioData(data)
//                    }
//                }
//            }
//        }.start()
//
//        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()



//        val uri = getPublicUri()
//        outputStream = contentResolver.openOutputStream(uri!!)!!
//        outputFileDescriptor = (outputStream as FileOutputStream).fd
//
//
//
//        mediaRecorder = MediaRecorder().apply {
//            setAudioSource(MediaRecorder.AudioSource.MIC)
//            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
//            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
//            setOutputFile(outputFileDescriptor)
//            prepare()
//            start()
//        }
//
//        isRecording = true
//        recordButton.setImageResource(R.drawable.pause_icon)
//        seconds = 0
//        handler.post(updateTimer)
//        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()

//    }


//    private fun stopRecording(){
//        isRecording = false
//        audioRecord?.apply {
//            stop()
//            release()
//        }
//        audioRecord = null
//
//        recordButton.setImageResource(R.drawable.mic_icon)
//        handler.removeCallbacks(updateTimer)
//        timerText.text = "00:00"
//        Toast.makeText(this, "Recording stopped", Toast.LENGTH_LONG).show()


//        mediaRecorder?.apply {
//            stop()
//            release()
//        }
//        mediaRecorder = null
//        isRecording = false
//        recordButton.setImageResource(R.drawable.mic_icon)
//        handler.removeCallbacks(updateTimer)
//
//        timerText.text = "00:00"
//        Toast.makeText(this, "Recording saved!", Toast.LENGTH_LONG).show()
//
//        outputStream.close()

//    }
private fun getPublicFile(fileName: String): File {
    val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
    val recordingsDir = File(musicDir, "Recordings")
    if (!recordingsDir.exists()) recordingsDir.mkdirs()
    return File(recordingsDir, fileName)
}

    fun startRecording() {

        timerText.text = "00:00"
        seconds = 0

        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        outputFile = getPublicFile("REC_${System.currentTimeMillis()}.wav")

        audioRecord?.startRecording()
        isRecording = true

        handler.post(updateTimer)

        recordingThread = Thread {
            writeAudioDataToFile(outputFile, bufferSize, sampleRate)
        }.also { it.start() }
    }

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        recordingThread = null
        handler.removeCallbacks(updateTimer)

        Toast.makeText(this, "Saved to: ${outputFile.absolutePath}", Toast.LENGTH_LONG).show()
    }

    private fun writeAudioDataToFile(file: File, bufferSize: Int, sampleRate: Int) {
        val buffer = ShortArray(bufferSize)
        val fos = FileOutputStream(file)
        val bos = BufferedOutputStream(fos)

        // Write placeholder WAV header
        val header = ByteArray(44)
        bos.write(header)

        var totalAudioLen = 0L
        while (isRecording) {
            val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            if (read > 0) {
                // Update waveform
                audioWaveView.post { audioWaveView.updateAudioData(buffer.copyOf(read)) }

                // Write PCM as bytes
                val byteBuffer = ByteBuffer.allocate(read * 2).order(ByteOrder.LITTLE_ENDIAN)
                buffer.take(read).forEach { byteBuffer.putShort(it) }
                bos.write(byteBuffer.array())
                totalAudioLen += read * 2
            }
        }

        bos.flush()
        bos.close()

        // Fix WAV header
        val totalDataLen = totalAudioLen + 36
        val byteRate = 16 * sampleRate * 1 / 8
        val raf = RandomAccessFile(file, "rw")
        writeWavHeader(raf, totalAudioLen, totalDataLen, sampleRate, 1, byteRate)
        raf.close()
    }

    private fun writeWavHeader(
        raf: RandomAccessFile,
        totalAudioLen: Long,
        totalDataLen: Long,
        sampleRate: Int,
        channels: Int,
        byteRate: Int
    ) {
        val header = ByteArray(44)

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (2 * channels).toByte()
        header[33] = 0
        header[34] = 16
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        raf.seek(0)
        raf.write(header, 0, 44)
    }


}