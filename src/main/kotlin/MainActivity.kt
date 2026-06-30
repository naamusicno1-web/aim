package com.tacticalgame.shooter

import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {
    
    private lateinit var gameView: GameView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        gameView = findViewById(R.id.game_view)
        setupControls()
    }
    
    private fun setupControls() {
        val aimbotSwitch = findViewById<SwitchMaterial>(R.id.switch_aimbot)
        aimbotSwitch.setOnCheckedChangeListener { _, isChecked ->
            gameView.toggleAimbot()
            Toast.makeText(this, "Aimbot: ${if (isChecked) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        }
        
        val espSwitch = findViewById<SwitchMaterial>(R.id.switch_esp)
        espSwitch.setOnCheckedChangeListener { _, isChecked ->
            gameView.toggleESP()
            Toast.makeText(this, "ESP: ${if (isChecked) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        }
        
        val autoFireSwitch = findViewById<SwitchMaterial>(R.id.switch_auto_fire)
        autoFireSwitch.setOnCheckedChangeListener { _, isChecked ->
            gameView.toggleAutoFire()
            Toast.makeText(this, "Auto Fire: ${if (isChecked) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        }
        
        val sensitivitySeek = findViewById<SeekBar>(R.id.seek_sensitivity)
        sensitivitySeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val sensitivity = (progress + 1) / 10f
                gameView.setAimbotSensitivity(sensitivity)
                val label = findViewById<TextView>(R.id.text_sensitivity)
                label.text = "Aimbot Sensitivity: %.1fx".format(sensitivity)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        val espRangeSeek = findViewById<SeekBar>(R.id.seek_esp_range)
        espRangeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val range = 50f + progress * 2f
                gameView.setESPRange(range)
                val label = findViewById<TextView>(R.id.text_esp_range)
                label.text = "ESP Range: ${range.toInt()}m"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
}
