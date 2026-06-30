package com.tacticalgame.shooter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.SurfaceHolder
import kotlin.math.*

class GameView(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs), SurfaceHolder.Callback {
    
    private val gameState = GameState()
    private val aimbotSystem = AimbotSystem(gameState)
    private val espSystem = ESPSystem(gameState)
    
    private var gameThread: GameThread? = null
    private var isRunning = false
    
    private var touchX = 0f
    private var touchY = 0f
    private var isFiring = false
    
    private val playerPaint = Paint().apply { color = Color.GREEN; isAntiAlias = true }
    private val enemyPaint = Paint().apply { color = Color.RED; isAntiAlias = true }
    private val espPaint = Paint().apply { color = Color.YELLOW; strokeWidth = 2f; style = Paint.Style.STROKE }
    private val textPaint = Paint().apply { color = Color.WHITE; textSize = 28f }
    private val projectilePaint = Paint().apply { color = Color.YELLOW }
    private val aimlinePaint = Paint().apply { color = Color.CYAN; strokeWidth = 2f }
    
    init {
        holder.addCallback(this)
        initializeGame()
    }
    
    private fun initializeGame() {
        gameState.player.position = Vector2(width / 2f, height / 2f)
        repeat(5) { i ->
            val enemy = Player(
                id = i + 1,
                position = Vector2((Math.random() * width).toFloat(), (Math.random() * height).toFloat()),
                health = 50,
                maxHealth = 50,
                name = "Enemy ${i + 1}",
                isEnemy = true
            )
            gameState.enemies.add(enemy)
        }
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        isRunning = true
        gameThread = GameThread(holder, this)
        gameThread?.start()
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        gameThread?.join()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                touchX = event.x
                touchY = event.y
                isFiring = true
            }
            MotionEvent.ACTION_UP -> { isFiring = false }
        }
        return true
    }
    
    fun update(deltaTime: Float) {
        if (gameState.gameOver) return
        
        gameState.player.position = Vector2(touchX, touchY)
        
        if (gameState.aimbotEnabled) {
            val target = aimbotSystem.getTargetEnemy()
            if (target != null) gameState.player.rotation = aimbotSystem.aimAt(target)
        }
        
        if (gameState.autoFireEnabled || isFiring) fireProjectile()
        
        updateEnemies(deltaTime)
        updateProjectiles(deltaTime)
        checkCollisions()
        
        if (gameState.enemies.all { !it.isAlive }) {
            gameState.waveNumber++
            spawnEnemyWave()
        }
    }
    
    private fun updateEnemies(deltaTime: Float) {
        for (enemy in gameState.enemies) {
            if (!enemy.isAlive) continue
            val direction = (gameState.player.position - enemy.position).normalize()
            enemy.velocity = direction * 100f
            enemy.position = enemy.position + enemy.velocity * deltaTime
            if (Math.random() < 0.02) {
                val direction2 = (gameState.player.position - enemy.position).normalize()
                gameState.projectiles.add(
                    Projectile(
                        id = gameState.projectiles.size,
                        position = enemy.position,
                        velocity = direction2 * 600f,
                        damage = 15,
                        owner = enemy.id
                    )
                )
            }
        }
    }
    
    private fun updateProjectiles(deltaTime: Float) {
        gameState.projectiles.removeAll { projectile ->
            projectile.position = projectile.position + projectile.velocity * deltaTime
            projectile.lifetime -= deltaTime
            projectile.lifetime <= 0 || isOffScreen(projectile.position)
        }
    }
    
    private fun checkCollisions() {
        for (projectile in gameState.projectiles.toList()) {
            for (enemy in gameState.enemies) {
                if (!enemy.isAlive) continue
                if (projectile.owner == gameState.player.id && projectile.position.distance(enemy.position) < 20f) {
                    enemy.health -= projectile.damage
                    if (enemy.health <= 0) {
                        enemy.isAlive = false
                        gameState.kills++
                        gameState.score += 100
                    }
                    gameState.projectiles.remove(projectile)
                    break
                }
            }
        }
        for (projectile in gameState.projectiles.toList()) {
            if (projectile.owner != gameState.player.id && projectile.position.distance(gameState.player.position) < 15f) {
                gameState.player.health -= projectile.damage
                if (gameState.player.health <= 0) gameState.gameOver = true
                gameState.projectiles.remove(projectile)
            }
        }
    }
    
    private fun fireProjectile() {
        if (gameState.player.ammo <= 0) return
        gameState.projectiles.add(
            Projectile(
                id = gameState.projectiles.size,
                position = gameState.player.position,
                velocity = Vector2(cos(gameState.player.rotation) * 800f, sin(gameState.player.rotation) * 800f),
                owner = gameState.player.id
            )
        )
        gameState.player.ammo--
    }
    
    private fun spawnEnemyWave() {
        gameState.enemies.clear()
        repeat(3 + gameState.waveNumber) { i ->
            gameState.enemies.add(
                Player(
                    id = i + 1,
                    position = Vector2((Math.random() * width).toFloat(), (Math.random() * height).toFloat()),
                    health = 50 + gameState.waveNumber * 10,
                    maxHealth = 50 + gameState.waveNumber * 10,
                    name = "Enemy ${i + 1}",
                    isEnemy = true
                )
            )
        }
    }
    
    private fun isOffScreen(position: Vector2): Boolean {
        return position.x < -50 || position.x > width + 50 || position.y < -50 || position.y > height + 50
    }
    
    fun draw() {
        val canvas = holder.lockCanvas() ?: return
        try {
            canvas.drawColor(Color.BLACK)
            canvas.drawCircle(gameState.player.position.x, gameState.player.position.y, 15f, playerPaint)
            canvas.drawLine(
                gameState.player.position.x,
                gameState.player.position.y,
                gameState.player.position.x + cos(gameState.player.rotation) * 100f,
                gameState.player.position.y + sin(gameState.player.rotation) * 100f,
                aimlinePaint
            )
            for (enemy in gameState.enemies) {
                if (enemy.isAlive) {
                    canvas.drawCircle(enemy.position.x, enemy.position.y, 12f, enemyPaint)
                    val healthPercent = enemy.health.toFloat() / enemy.maxHealth
                    canvas.drawRect(
                        enemy.position.x - 15f,
                        enemy.position.y - 25f,
                        enemy.position.x - 15f + 30f * healthPercent,
                        enemy.position.y - 20f,
                        Paint().apply { color = Color.GREEN }
                    )
                }
            }
            if (gameState.espEnabled) {
                for (enemy in espSystem.getVisibleEnemies()) {
                    canvas.drawCircle(enemy.position.x, enemy.position.y, gameState.espRange, espPaint)
                }
            }
            for (projectile in gameState.projectiles) {
                canvas.drawCircle(projectile.position.x, projectile.position.y, 4f, projectilePaint)
            }
            drawHUD(canvas)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }
    
    private fun drawHUD(canvas: Canvas) {
        val hudPaint = Paint().apply { color = Color.WHITE; textSize = 32f }
        canvas.drawText("Score: ${gameState.score}", 20f, 50f, hudPaint)
        canvas.drawText("HP: ${gameState.player.health}", 20f, 100f, hudPaint)
        canvas.drawText("Ammo: ${gameState.player.ammo}", 20f, 150f, hudPaint)
        canvas.drawText("Wave: ${gameState.waveNumber}", width - 250f, 50f, hudPaint)
        canvas.drawText("Kills: ${gameState.kills}", width - 250f, 100f, hudPaint)
        val statusPaint = Paint().apply { color = if (gameState.aimbotEnabled) Color.GREEN else Color.RED; textSize = 24f }
        canvas.drawText("AIMBOT: ${if (gameState.aimbotEnabled) "ON" else "OFF"}", 20f, height - 50f, statusPaint)
        val espPaintStatus = Paint().apply { color = if (gameState.espEnabled) Color.GREEN else Color.RED; textSize = 24f }
        canvas.drawText("ESP: ${if (gameState.espEnabled) "ON" else "OFF"}", 20f, height - 20f, espPaintStatus)
        if (gameState.gameOver) {
            val gameOverPaint = Paint().apply { color = Color.RED; textSize = 80f }
            canvas.drawText("GAME OVER", width / 2 - 200f, height / 2f, gameOverPaint)
        }
    }
    
    fun toggleAimbot() { gameState.aimbotEnabled = !gameState.aimbotEnabled }
    fun toggleESP() { gameState.espEnabled = !gameState.espEnabled }
    fun toggleAutoFire() { gameState.autoFireEnabled = !gameState.autoFireEnabled }
    fun setAimbotSensitivity(sensitivity: Float) { gameState.aimbotSensitivity = sensitivity }
    fun setESPRange(range: Float) { gameState.espRange = range }
    
    private inner class GameThread(val holder: SurfaceHolder, val view: GameView) : Thread() {
        private var lastTime = System.currentTimeMillis()
        override fun run() {
            while (isRunning) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = (currentTime - lastTime) / 1000f
                lastTime = currentTime
                view.update(deltaTime.coerceAtMost(0.033f))
                view.draw()
            }
        }
    }
}
