package com.mkbot

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var statusText: TextView

    companion object {
        private const val REQUEST_SCREEN_CAPTURE = 1001
        private const val REQUEST_NOTIF_PERMISSION = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 100, 40, 40)
        }

        statusText = TextView(this).apply { text = "Status: aguardando" }
        root.addView(statusText)

        root.addView(Button(this).apply {
            text = "1. Ativar overlay"
            setOnClickListener { requestOverlayPermission() }
        })

        root.addView(Button(this).apply {
            text = "2. Ativar acessibilidade"
            setOnClickListener { openAccessibilitySettings() }
        })

        root.addView(Button(this).apply {
            text = "3. Iniciar captura de tela"
            setOnClickListener { requestScreenCapturePermission() }
        })

        setContentView(root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIF_PERMISSION)
        }
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else {
            updateStatus("overlay já ativado")
        }
    }

    private fun openAccessibilitySettings() {
        // BotAccessibilityService ainda não existe — quando existir, também
        // vamos conferir Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES aqui.
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        Toast.makeText(this, "Ative o MK Bot na lista de serviços", Toast.LENGTH_LONG).show()
    }

    private fun requestScreenCapturePermission() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_SCREEN_CAPTURE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK && data != null) {
                updateStatus("captura de tela: permissão concedida")
                // TODO: quando o ScreenCaptureService.kt existir, chamar aqui:
                // val intent = Intent(this, ScreenCaptureService::class.java).apply {
                //     putExtra("resultCode", resultCode)
                //     putExtra("data", data)
                // }
                // startForegroundService(intent)
            } else {
                updateStatus("captura de tela: negado")
            }
        }
    }

    private fun updateStatus(message: String) {
        statusText.text = "Status: $message"
    }
}
