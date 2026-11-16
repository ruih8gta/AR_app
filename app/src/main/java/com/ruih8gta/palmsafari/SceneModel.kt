package com.ruih8gta.palmsafari

import android.content.Context
import android.graphics.Color
import org.json.JSONArray
import kotlin.math.abs
import kotlin.math.sin

enum class Motion { IDLE, STEP, PAUSE }

data class SceneSettings(
    val model: Int = 0,
    val motion: Motion = Motion.IDLE,
    val size: Float = 1f,
    val angle: Float = -30f,
) {
    fun normalized() = copy(
        model = model.coerceIn(0, 1),
        size = if (size.isFinite()) size.coerceIn(.5f, 1.5f) else 1f,
        angle = if (angle.isFinite()) angle.coerceIn(-180f, 180f) else 0f,
    )
}

/** Uses foreground frame deltas, so pausing/resuming never jumps the animation. */
class MotionClock {
    var seconds = 0f
        private set
    var pose = Motion.IDLE
        private set
    private var previous: Long? = null
    fun tick(now: Long, motion: Motion) {
        val delta = previous?.let { ((now - it) / 1_000_000_000f).coerceIn(0f, .05f) } ?: 0f
        previous = now
        if (motion != Motion.PAUSE) { seconds += delta; pose = motion }
    }
    fun suspendClock() { previous = null }
}

data class JointPose(val y: Float = 0f, val z: Float = 0f, val rotateX: Float = 0f, val rotateY: Float = 0f, val scaleY: Float = 1f)

object AnimationPose {
    fun rootY(t: Float, motion: Motion) = if (motion == Motion.STEP) abs(sin(t * 6f)) * .025f else 0f
    fun joint(id: String, t: Float, motion: Motion): JointPose {
        if (id == "tail") return JointPose(rotateY = sin(t * 2.3f) * 10.3f)
        if (id == "head") return JointPose(rotateX = sin(t * 1.7f) * 2f)
        if (id == "body") return JointPose(scaleY = 1f + sin(t * 2f) * .012f)
        if (motion == Motion.STEP && id in setOf("legL", "legR", "backL", "backR")) {
            val stride = sin(t * 6f) * if (id == "legL" || id == "backR") 1f else -1f
            return JointPose(y = stride.coerceAtLeast(0f) * .065f, z = stride * .06f)
        }
        return JointPose()
    }
}

data class Part(val position: FloatArray, val scale: FloatArray, val color: FloatArray, val cone: Boolean, val rotation: Float)
data class Joint(val id: String, val pivot: FloatArray, val parts: List<Part>)
data class Creature(val name: String, val joints: List<Joint>)

fun loadCreatures(context: Context): List<Creature> {
    fun JSONArray.vector() = FloatArray(3) { getDouble(it).toFloat() }
    val data = JSONArray(context.assets.open("creatures.json").bufferedReader().use { it.readText() })
    return List(data.length()) { index ->
        val model = data.getJSONObject(index)
        val groups = model.getJSONArray("groups")
        Creature(model.getString("name"), List(groups.length()) { n ->
            val group = groups.getJSONObject(n)
            val parts = group.getJSONArray("parts")
            Joint(group.getString("id"), group.getJSONArray("pivot").vector(), List(parts.length()) { p ->
                val part = parts.getJSONObject(p)
                val color = Color.parseColor(part.getString("c"))
                Part(part.getJSONArray("p").vector(), part.getJSONArray("s").vector(),
                    floatArrayOf(Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f, 1f),
                    part.optString("shape") == "cone", part.optDouble("r", 0.0).toFloat())
            })
        })
    }
}
