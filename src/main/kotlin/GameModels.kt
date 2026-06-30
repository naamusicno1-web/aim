package com.tacticalgame.shooter

import kotlin.math.*

data class Vector2(val x: Float, val y: Float) {
    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vector2(x * scalar, y * scalar)
    
    fun distance(other: Vector2): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
    
    fun normalize(): Vector2 {
        val len = sqrt(x * x + y * y)
        return if (len > 0) Vector2(x / len, y / len) else Vector2(0f, 0f)
    }
}

data class Player(
    val id: Int,
    var position: Vector2,
    var velocity: Vector2 = Vector2(0f, 0f),
    var health: Int = 100,
    var maxHealth: Int = 100,
    var rotation: Float = 0f,
    var ammo: Int = 120,
    var isAlive: Boolean = true,
    var name: String = "Player",
    var isEnemy: Boolean = false
)

data class Projectile(
    val id: Int,
    var position: Vector2,
    val velocity: Vector2,
    val damage: Int = 25,
    var lifetime: Float = 5f,
    val owner: Int
)

data class GameState(
    var player: Player = Player(0, Vector2(400f, 300f)),
    var enemies: MutableList<Player> = mutableListOf(),
    var projectiles: MutableList<Projectile> = mutableListOf(),
    var aimbotEnabled: Boolean = false,
    var espEnabled: Boolean = false,
    var aimbotSensitivity: Float = 1.5f,
    var espRange: Float = 200f,
    var autoFireEnabled: Boolean = false,
    var score: Int = 0,
    var kills: Int = 0,
    var gameOver: Boolean = false,
    var waveNumber: Int = 1
)

class AimbotSystem(private val gameState: GameState) {
    
    fun getTargetEnemy(): Player? {
        if (!gameState.aimbotEnabled || gameState.enemies.isEmpty()) return null
        
        val player = gameState.player
        var closestEnemy: Player? = null
        var closestDistance = Float.MAX_VALUE
        
        for (enemy in gameState.enemies) {
            if (!enemy.isAlive) continue
            
            val distance = player.position.distance(enemy.position)
            val rangeMultiplier = 300f * gameState.aimbotSensitivity
            
            if (distance < closestDistance && distance < rangeMultiplier) {
                closestDistance = distance
                closestEnemy = enemy
            }
        }
        
        return closestEnemy
    }
    
    fun aimAt(target: Player): Float {
        val player = gameState.player
        val dx = target.position.x - player.position.x
        val dy = target.position.y - player.position.y
        return atan2(dy, dx)
    }
}

class ESPSystem(private val gameState: GameState) {
    
    fun getVisibleEnemies(): List<Player> {
        if (!gameState.espEnabled) return emptyList()
        return gameState.enemies.filter { enemy ->
            enemy.isAlive && gameState.player.position.distance(enemy.position) < gameState.espRange
        }
    }
    
    fun getEnemyInfo(enemy: Player): String {
        val distance = gameState.player.position.distance(enemy.position)
        return "${enemy.name}\nHP: ${enemy.health}\nDist: ${distance.toInt()}m"
    }
}
