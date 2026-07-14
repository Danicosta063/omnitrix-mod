package com.danicosta.mkbot

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile

class MainActivity : ComponentActivity() {

    companion object {
        const val ACCESSIBILITY_SERVICE_ID = "com.danicosta.mkbot/com.danicosta.mkbot.BotAccessibilityService"
        const val PREFS_NAME = "mkbot_prefs"
        const val KEY_FOLDER_URI = "memory_folder_uri"
        const val CURRENT_CHARACTER = "Ashrah"

        var projectionResultCode: Int = 0
        var projectionResultData: Intent? = null
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var statusAccessibility: TextView
    private lateinit var statusCapture: TextView
    private lateinit var statusFolder: TextView
    private lateinit var timeCharacter: TextView
    private lateinit var timeTotal: TextView

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val captureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                projectionResultCode = result.resultCode
                projectionResultData = result.data
                ContextCompat.startForegroundService(this, Intent(this, ScreenCaptureService::class.java))
                Toast.makeText(this, "Captura de tela liberada", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Captura de tela negada", Toast.LENGTH_SHORT).show()
            }
            refreshStatus()
        }

    private val folderLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                prefs.edit().putString(KEY_FOLDER_URI, uri.toString()).apply()
                MemoryManager.load()
                Toast.makeText(this, "Pasta de memória escolhida, carregando...", Toast.LENGTH_SHORT).show()
            }
            refreshStatus()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        MemoryManager.onLoaded = { refreshStatus() }
        MemoryManager.init(applicationContext)
        setContentView(buildLayout())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun buildLayout(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val title = TextView(this).apply {
            text = "MK Armageddon Bot"
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }
        root.addView(title)

        statusAccessibility = TextView(this).apply { textSize = 16f }
        statusCapture = TextView(this).apply { textSize = 16f }
        statusFolder = TextView(this).apply { textSize = 16f }
        root.addView(statusAccessibility)
        root.addView(statusCapture)
        root.addView(statusFolder)

        root.addView(spacer())

        val btnAccessibility = Button(this).apply {
            text = "Ativar Acessibilidade"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        val btnCapture = Button(this).apply {
            text = "Permitir Captura de Tela"
            setOnClickListener {
                val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                captureLauncher.launch(manager.createScreenCaptureIntent())
            }
        }
        val btnFolder = Button(this).apply {
            text = "Escolher Pasta de Memória"
            setOnClickListener { folderLauncher.launch(null) }
        }
        val btnDebugFrame = Button(this).apply {
            text = "Salvar Captura Atual (diagnóstico)"
            setOnClickListener { saveDebugFrame() }
        }
        root.addView(btnAccessibility)
        root.addView(btnCapture)
        root.addView(btnFolder)
        root.addView(btnDebugFrame)

        root.addView(spacer())

        val timerTitle = TextView(this).apply {
            text = "Tempo jogado"
            textSize = 18f
            setPadding(0, 32, 0, 16)
        }
        root.addView(timerTitle)

        timeCharacter = TextView(this).apply { textSize = 16f }
        timeTotal = TextView(this).apply { textSize = 16f }
        root.addView(timeCharacter)
        root.addView(timeTotal)

        return root
    }

    private fun spacer(): TextView = TextView(this).apply {
        text = ""
        setPadding(0, 24, 0, 24)
    }

    private fun saveDebugFrame() {
        val frame = ScreenCaptureService.latestFrame
        if (frame == null) {
            Toast.makeText(this, "Nenhum quadro capturado ainda — a captura pode não estar ativa de verdade", Toast.LENGTH_LONG).show()
            return
        }
        val folderUriString = prefs.getString(KEY_FOLDER_URI, null)
        if (folderUriString == null) {
            Toast.makeText(this, "Escolhe a pasta de memória primeiro", Toast.LENGTH_SHORT).show()
            return
        }
        val root = DocumentFile.fromTreeUri(this, Uri.parse(folderUriString))
        val fileName = "mkbot_debug_frame.png"
        root?.findFile(fileName)?.delete()
        val newFile = root?.createFile("image/png", fileName)
        if (newFile != null) {
            contentResolver.openOutputStream(newFile.uri)?.use { out ->
                frame.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Toast.makeText(this, "Print salvo na pasta de memória: $fileName", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Erro ao salvar o print", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshStatus() {
        val enabledServices =
            Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val accessibilityOn = enabledServices.contains(ACCESSIBILITY_SERVICE_ID)
        statusAccessibility.text = "Acessibilidade: " + if (accessibilityOn) "ativada" else "desativada"

        statusCapture.text = "Captura de tela: " + if (ScreenCaptureService.isCapturing) "ativa" else "não liberada"

        val folderUri = prefs.getString(KEY_FOLDER_URI, null)
        statusFolder.text = "Pasta de memória: " + if (folderUri != null) "escolhida" else "não escolhida"

        timeCharacter.text = "$CURRENT_CHARACTER: " + formatTime(MemoryManager.characterSeconds(CURRENT_CHARACTER))
        timeTotal.text = "Total: " + formatTime(MemoryManager.totalSeconds())
    }

    private fun formatTime(totalSeconds: Long): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }
}
