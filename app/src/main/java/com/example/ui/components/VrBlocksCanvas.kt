package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.VrEnvironmentMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VrBlocksStage(
    accentColor: Color,
    isVrSplitMode: Boolean,
    selectedBlockIndex: Int?,
    isAnswerCorrect: Boolean?,
    environmentMode: VrEnvironmentMode = VrEnvironmentMode.RUANG_KELAS,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var gyroYaw by remember { mutableFloatStateOf(0f) }
    var gyroPitch by remember { mutableFloatStateOf(0f) }
    var dragYaw by remember { mutableFloatStateOf(0f) }
    var dragPitch by remember { mutableFloatStateOf(0f) }

    val renderer = remember { VrBlocksGlRenderer() }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    gyroYaw = (event.values.getOrNull(1) ?: 0f) * 52f
                    gyroPitch = ((event.values.getOrNull(0) ?: 0f) * 32f).coerceIn(-35f, 35f)
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    gyroYaw = (event.values.getOrNull(0) ?: 0f) * 4.0f
                    gyroPitch = ((event.values.getOrNull(1) ?: 0f) * 2.5f).coerceIn(-30f, 30f)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (rotSensor != null) {
            sensorManager?.registerListener(listener, rotSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val glSurfaceView = remember(context) {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(lifecycleOwner, glSurfaceView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glSurfaceView.onResume()
                Lifecycle.Event.ON_PAUSE -> glSurfaceView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glSurfaceView.onPause()
        }
    }

    renderer.updateState(
        accent = accentColor,
        vrSplit = isVrSplitMode,
        envMode = environmentMode,
        selectedIdx = selectedBlockIndex,
        correct = isAnswerCorrect,
        yawDeg = gyroYaw + dragYaw,
        pitchDeg = gyroPitch + dragPitch
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0B1426))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    dragYaw += dragAmount.x * 0.35f
                    dragPitch = (dragPitch + dragAmount.y * 0.25f).coerceIn(-40f, 40f)
                }
            }
    ) {
        AndroidView(
            factory = { glSurfaceView },
            modifier = Modifier.fillMaxSize()
        )

        if (isVrSplitMode) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text("+", color = Color.White.copy(alpha = 0.65f), fontSize = 16.sp)
                }
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.45f))
                )
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text("+", color = Color.White.copy(alpha = 0.65f), fontSize = 16.sp)
                }
            }
        }

        Text(
            text = "${environmentMode.label.uppercase()} • ${if (isVrSplitMode) "VR STEREO 3D" else "3D XR"} • GESER / MIRINGKAN HP",
            color = Color.White.copy(alpha = 0.90f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .padding(bottom = 2.dp)
        )
    }
}

private class VrBlocksGlRenderer : GLSurfaceView.Renderer {

    @Volatile private var accentR = 0.15f
    @Volatile private var accentG = 0.39f
    @Volatile private var accentB = 0.92f
    @Volatile private var isVrSplit = false
    @Volatile private var envMode = VrEnvironmentMode.RUANG_KELAS
    @Volatile private var selectedBlock: Int? = null
    @Volatile private var answerCorrect: Boolean? = null
    @Volatile private var userYawDeg = 0f
    @Volatile private var userPitchDeg = 0f

    private var surfaceWidth = 1
    private var surfaceHeight = 1
    private var startTimeMs = System.currentTimeMillis()

    private var program = 0
    private var posHandle = 0
    private var normalHandle = 0
    private var mvpHandle = 0
    private var modelHandle = 0
    private var colorHandle = 0

    private lateinit var cubePosBuffer: FloatBuffer
    private lateinit var cubeNormalBuffer: FloatBuffer

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    fun updateState(
        accent: Color,
        vrSplit: Boolean,
        envMode: VrEnvironmentMode,
        selectedIdx: Int?,
        correct: Boolean?,
        yawDeg: Float,
        pitchDeg: Float
    ) {
        accentR = accent.red
        accentG = accent.green
        accentB = accent.blue
        isVrSplit = vrSplit
        this.envMode = envMode
        selectedBlock = selectedIdx
        answerCorrect = correct
        userYawDeg = yawDeg
        userPitchDeg = pitchDeg
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        startTimeMs = System.currentTimeMillis()

        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            attribute vec4 aPosition;
            attribute vec3 aNormal;
            varying vec3 vNormal;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vNormal = normalize((uModelMatrix * vec4(aNormal, 0.0)).xyz);
            }
        """.trimIndent()

        val fragmentShaderCode = """
            precision mediump float;
            uniform vec4 uColor;
            varying vec3 vNormal;
            void main() {
                vec3 lightDir = normalize(vec3(0.45, 0.90, 0.65));
                float diff = max(dot(normalize(vNormal), lightDir), 0.0);
                vec3 lit = uColor.rgb * (0.45 + 0.55 * diff);
                gl_FragColor = vec4(lit, uColor.a);
            }
        """.trimIndent()

        val vShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vShader)
            GLES20.glAttachShader(it, fShader)
            GLES20.glLinkProgram(it)
        }

        posHandle = GLES20.glGetAttribLocation(program, "aPosition")
        normalHandle = GLES20.glGetAttribLocation(program, "aNormal")
        mvpHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        modelHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")

        initCubeBuffers()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (envMode == VrEnvironmentMode.HALAMAN_SEKOLAH) {
            GLES20.glClearColor(0.28f, 0.66f, 0.95f, 1.0f) // Cerah langit halaman sekolah
        } else {
            GLES20.glClearColor(0.11f, 0.16f, 0.28f, 1.0f) // Suasana ruang kelas 3D
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        val elapsedSec = (System.currentTimeMillis() - startTimeMs) / 1000f

        if (isVrSplit) {
            val halfW = (surfaceWidth / 2).coerceAtLeast(1)
            GLES20.glViewport(0, 0, halfW, surfaceHeight)
            renderSceneForEye(eyeOffset = -0.15f, vpW = halfW, vpH = surfaceHeight, t = elapsedSec)

            GLES20.glViewport(halfW, 0, (surfaceWidth - halfW).coerceAtLeast(1), surfaceHeight)
            renderSceneForEye(eyeOffset = 0.15f, vpW = (surfaceWidth - halfW).coerceAtLeast(1), vpH = surfaceHeight, t = elapsedSec)
        } else {
            GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
            renderSceneForEye(eyeOffset = 0f, vpW = surfaceWidth, vpH = surfaceHeight, t = elapsedSec)
        }
    }

    private fun renderSceneForEye(eyeOffset: Float, vpW: Int, vpH: Int, t: Float) {
        val ratio = vpW.toFloat() / vpH.coerceAtLeast(1).toFloat()
        Matrix.perspectiveM(projMatrix, 0, 56f, ratio, 0.5f, 60f)

        val camYawRad = Math.toRadians(userYawDeg.toDouble())
        val camPitchRad = Math.toRadians(userPitchDeg.toDouble())
        val lookX = (sin(camYawRad) * 1.1).toFloat()
        val lookY = (-sin(camPitchRad) * 0.7).toFloat()

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyeOffset, 1.35f, 6.4f,
            lookX, lookY + 0.15f, -1.0f,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        if (envMode == VrEnvironmentMode.RUANG_KELAS) {
            drawClassroomEnvironment()
        } else {
            drawSchoolyardEnvironment(t)
        }

        // Center Subject Core Quiz Cube
        val (cr, cg, cb) = when (answerCorrect) {
            true -> Triple(0.06f, 0.78f, 0.52f)
            false -> Triple(0.95f, 0.25f, 0.37f)
            null -> Triple(accentR, accentG, accentB)
        }
        drawCube(
            tx = 0f,
            ty = 0.55f + sin(t * 2.2f) * 0.12f,
            tz = 0f,
            sx = 0.85f, sy = 0.85f, sz = 0.85f,
            rotY = t * 42f + userYawDeg * 0.4f,
            rotX = 16f + userPitchDeg * 0.4f,
            r = cr, g = cg, b = cb
        )

        // 4 Floating Answer Cubes (A, B, C, D)
        val blockColors = listOf(
            Triple(0.23f, 0.51f, 0.96f), // A
            Triple(0.06f, 0.72f, 0.51f), // B
            Triple(0.96f, 0.62f, 0.04f), // C
            Triple(0.93f, 0.28f, 0.60f)  // D
        )
        for (i in 0..3) {
            val angle = t * 0.75f + i * (Math.PI.toFloat() / 2f)
            val bx = cos(angle) * 2.15f
            val bz = sin(angle) * 1.35f
            val by = 0.45f + sin(t * 2.6f + i) * 0.18f
            val isSel = selectedBlock == i
            val scale = if (isSel) 0.64f else 0.46f
            val (br, bg, bb) = when {
                isSel && answerCorrect == true -> Triple(0.06f, 0.85f, 0.52f)
                isSel && answerCorrect == false -> Triple(0.95f, 0.25f, 0.37f)
                else -> blockColors[i]
            }
            drawCube(
                tx = bx, ty = by, tz = bz,
                sx = scale, sy = scale, sz = scale,
                rotY = t * 60f + i * 45f,
                rotX = t * 24f,
                r = br, g = bg, b = bb
            )
        }
    }

    private fun drawClassroomEnvironment() {
        // Lantai Keramik Kelas
        drawCube(0f, -1.6f, -1.0f, 12.0f, 0.12f, 12.0f, 0f, 0f, 0.88f, 0.86f, 0.80f)
        // Dinding Belakang Kelas
        drawCube(0f, 1.2f, -5.2f, 12.0f, 5.6f, 0.2f, 0f, 0f, 0.96f, 0.94f, 0.86f)
        // Dinding Samping Kiri & Kanan
        drawCube(-5.8f, 1.2f, -1.0f, 0.2f, 5.6f, 10.0f, 0f, 0f, 0.90f, 0.89f, 0.82f)
        drawCube(5.8f, 1.2f, -1.0f, 0.2f, 5.6f, 10.0f, 0f, 0f, 0.90f, 0.89f, 0.82f)
        // Bingkai Kayu & Papan Tulis Hijau
        drawCube(0f, 1.35f, -5.05f, 5.6f, 2.3f, 0.1f, 0f, 0f, 0.52f, 0.32f, 0.16f)
        drawCube(0f, 1.35f, -4.98f, 5.2f, 2.0f, 0.08f, 0f, 0f, 0.09f, 0.36f, 0.24f)
        // Meja Guru di depan papan tulis
        drawCube(0f, -1.05f, -3.7f, 1.8f, 0.85f, 0.8f, 0f, 0f, 0.62f, 0.40f, 0.22f)
        // 4 Meja & Kursi Siswa 3D di Ruang Kelas
        val deskCoords = listOf(-2.4f to -1.8f, 2.4f to -1.8f, -2.4f to 1.6f, 2.4f to 1.6f)
        deskCoords.forEach { (dx, dz) ->
            // Meja kayu siswa
            drawCube(dx, -0.95f, dz, 1.15f, 0.12f, 0.72f, 0f, 0f, 0.76f, 0.53f, 0.30f)
            drawCube(dx - 0.45f, -1.28f, dz, 0.10f, 0.6f, 0.55f, 0f, 0f, 0.45f, 0.48f, 0.55f)
            drawCube(dx + 0.45f, -1.28f, dz, 0.10f, 0.6f, 0.55f, 0f, 0f, 0.45f, 0.48f, 0.55f)
            // Kursi siswa
            drawCube(dx, -1.25f, dz + 0.62f, 0.62f, 0.10f, 0.48f, 0f, 0f, 0.20f, 0.45f, 0.85f)
            drawCube(dx, -0.95f, dz + 0.82f, 0.62f, 0.55f, 0.08f, 0f, 0f, 0.20f, 0.45f, 0.85f)
        }
    }

    private fun drawSchoolyardEnvironment(t: Float) {
        // Halaman Rumput Hijau Luas
        drawCube(0f, -1.65f, -4.0f, 28.0f, 0.15f, 28.0f, 0f, 0f, 0.20f, 0.68f, 0.32f)
        // Lapangan Upacara Paving di Tengah
        drawCube(0f, -1.56f, -1.0f, 7.6f, 0.08f, 8.2f, 0f, 0f, 0.78f, 0.80f, 0.82f)
        // Gedung Sekolah Utama di Latar Belakang
        drawCube(0f, 0.6f, -8.5f, 14.0f, 4.4f, 3.0f, 0f, 0f, 0.96f, 0.94f, 0.88f)
        // Atap Genteng Merah Sekolah
        drawCube(0f, 3.1f, -8.5f, 15.0f, 1.0f, 3.6f, 0f, 0f, 0.82f, 0.24f, 0.18f)
        // Pintu & Jendela Kelas
        drawCube(0f, -0.35f, -6.95f, 1.4f, 2.3f, 0.12f, 0f, 0f, 0.46f, 0.28f, 0.14f)
        listOf(-4.5f, -2.3f, 2.3f, 4.5f).forEach { wx ->
            drawCube(wx, 0.7f, -6.95f, 1.2f, 1.1f, 0.12f, 0f, 0f, 0.35f, 0.75f, 0.95f)
        }
        // Tiang Bendera & Bendera Merah Putih
        drawCube(-3.8f, 0.9f, -4.2f, 0.12f, 5.0f, 0.12f, 0f, 0f, 0.90f, 0.92f, 0.95f)
        val wave = sin(t * 3.0f) * 4f
        drawCube(-3.2f, 3.15f, -4.2f, 1.1f, 0.35f, 0.06f, wave, 0f, 0.90f, 0.12f, 0.16f)
        drawCube(-3.2f, 2.80f, -4.2f, 1.1f, 0.35f, 0.06f, wave, 0f, 0.98f, 0.98f, 0.98f)
        // Pepohonan di Halaman Sekolah Luas
        val treePos = listOf(-6.8f to -3.5f, 6.8f to -3.5f, -6.5f to 1.8f, 6.5f to 1.8f)
        treePos.forEach { (tx, tz) ->
            drawCube(tx, -0.5f, tz, 0.38f, 2.2f, 0.38f, 0f, 0f, 0.45f, 0.28f, 0.15f)
            drawCube(tx, 1.05f, tz, 1.5f, 1.6f, 1.5f, 0f, 0f, 0.12f, 0.56f, 0.24f)
        }
    }

    private fun drawCube(
        tx: Float, ty: Float, tz: Float,
        sx: Float, sy: Float, sz: Float,
        rotY: Float, rotX: Float,
        r: Float, g: Float, b: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, tx, ty, tz)
        Matrix.rotateM(modelMatrix, 0, rotY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotX, 1f, 0f, 0f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)

        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, cubePosBuffer)

        GLES20.glEnableVertexAttribArray(normalHandle)
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, cubeNormalBuffer)

        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(modelHandle, 1, false, modelMatrix, 0)
        GLES20.glUniform4f(colorHandle, r, g, b, 1.0f)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36)

        GLES20.glDisableVertexAttribArray(posHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
    }

    private fun compileShader(type: Int, code: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, code)
            GLES20.glCompileShader(shader)
        }
    }

    private fun initCubeBuffers() {
        val h = 0.5f
        val positions = floatArrayOf(
            -h, -h, h, h, -h, h, h, h, h,
            -h, -h, h, h, h, h, -h, h, h,
            h, -h, -h, -h, -h, -h, -h, h, -h,
            h, -h, -h, -h, h, -h, h, h, -h,
            -h, -h, -h, -h, -h, h, -h, h, h,
            -h, -h, -h, -h, h, h, -h, h, -h,
            h, -h, h, h, -h, -h, h, h, -h,
            h, -h, h, h, h, -h, h, h, h,
            -h, h, h, h, h, h, h, h, -h,
            -h, h, h, h, h, -h, -h, h, -h,
            -h, -h, -h, h, -h, -h, h, -h, h,
            -h, -h, -h, h, -h, h, -h, -h, h
        )
        val normals = floatArrayOf(
            0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f,
            0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f,
            0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f,
            0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f,
            -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f,
            -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f,
            1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f,
            1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f,
            0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f,
            0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f,
            0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f,
            0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f
        )
        cubePosBuffer = ByteBuffer.allocateDirect(positions.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(positions); position(0) }
        cubeNormalBuffer = ByteBuffer.allocateDirect(normals.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(normals); position(0) }
    }
}
