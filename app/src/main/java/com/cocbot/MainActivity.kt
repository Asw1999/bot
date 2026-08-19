package com.cocbot

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.cocbot.capture.ScreenCaptureService
import com.cocbot.core.*
import com.cocbot.detection.*
import com.cocbot.input.BotAccessibilityService
import com.cocbot.navigation.Navigator
import com.cocbot.overlay.OverlayManager
import com.cocbot.strategy.SimpleDeployer
import com.cocbot.util.BotLog
import com.cocbot.util.CoordScaler
import com.cocbot.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var config: BotConfig
    private var overlay: OverlayManager? = null
    private var botEngine: BotEngine? = null

    private val captureRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenCaptureService.startCapture(this, result.resultCode, result.data!!)
            updateServiceStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        config = BotConfig.load(this)
        setupUI()
        updateServiceStatus()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun setupUI() {
        // Deploy side spinner
        val sides = DeploySide.entries.map { it.name }
        binding.spDeploySide.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sides)
        binding.spDeploySide.setSelection(config.deploySide.ordinal)

        // Deploy pattern spinner
        val patterns = DeployPattern.entries.map { it.name }
        binding.spDeployPattern.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, patterns)
        binding.spDeployPattern.setSelection(config.deployPattern.ordinal)

        // Load config into fields
        binding.etTargetGold.setText(config.targetGold.toString())
        binding.etTargetElixir.setText(config.targetElixir.toString())
        binding.etTargetDark.setText(config.targetDarkElixir.toString())
        binding.etMaxAttacks.setText(config.maxAttacks.toString())

        // Buttons
        binding.btnOpenAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnStartCapture.setOnClickListener {
            if (ScreenCaptureService.instance != null) {
                ScreenCaptureService.stopCapture(this)
                updateServiceStatus()
            } else {
                val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                captureRequest.launch(mpm.createScreenCaptureIntent())
            }
        }

        binding.btnSaveConfig.setOnClickListener { saveConfig() }

        binding.btnLaunchOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
                return@setOnClickListener
            }
            launchOverlay()
        }
    }

    private fun saveConfig() {
        config.targetGold = binding.etTargetGold.text.toString().toIntOrNull() ?: 200000
        config.targetElixir = binding.etTargetElixir.text.toString().toIntOrNull() ?: 200000
        config.targetDarkElixir = binding.etTargetDark.text.toString().toIntOrNull() ?: 0
        config.deploySide = DeploySide.entries[binding.spDeploySide.selectedItemPosition]
        config.deployPattern = DeployPattern.entries[binding.spDeployPattern.selectedItemPosition]
        config.maxAttacks = binding.etMaxAttacks.text.toString().toIntOrNull() ?: 50
        config.save(this)
        Toast.makeText(this, "Config saved", Toast.LENGTH_SHORT).show()
    }

    private fun launchOverlay() {
        saveConfig()

        val input = BotAccessibilityService.instance
        val capture = ScreenCaptureService.instance

        if (input == null) {
            Toast.makeText(this, "Enable Accessibility Service first", Toast.LENGTH_LONG).show()
            return
        }
        if (capture == null) {
            Toast.makeText(this, "Start Screen Capture first", Toast.LENGTH_LONG).show()
            return
        }

        val scaler = CoordScaler(this)
        val matcher = TemplateMatcher(this)
        val detector = TemplateDetector(matcher)
        val lootReader = LootReader()
        val navigator = Navigator(detector, lootReader, capture, input, scaler)
        val strategy = SimpleDeployer(scaler)

        overlay = OverlayManager(this).apply {
            onStartClick = {
                botEngine = BotEngine(config, capture, input, detector, navigator, strategy, this)
                botEngine?.start()
            }
            onStopClick = {
                botEngine?.stop()
            }
            show()
        }

        BotLog.i("Overlay launched")
        // Minimize app to show overlay on top of game
        moveTaskToBack(true)
    }

    private fun updateServiceStatus() {
        val accOn = BotAccessibilityService.isConnected
        binding.tvAccessibilityStatus.text = if (accOn) "Accessibility: ✅ ON" else "Accessibility: ❌ OFF"
        binding.tvAccessibilityStatus.setTextColor(if (accOn) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())

        val capOn = ScreenCaptureService.instance != null
        binding.tvCaptureStatus.text = if (capOn) "Screen Capture: ✅ ON" else "Screen Capture: ❌ OFF"
        binding.tvCaptureStatus.setTextColor(if (capOn) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
        binding.btnStartCapture.text = if (capOn) "Stop Screen Capture" else "Start Screen Capture"
    }

    override fun onDestroy() {
        botEngine?.stop()
        overlay?.hide()
        super.onDestroy()
    }
}