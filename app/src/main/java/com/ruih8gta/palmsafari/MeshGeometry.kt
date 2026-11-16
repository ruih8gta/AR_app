package com.ruih8gta.palmsafari

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Interleaved vertices/normals, flat shaded. Pure Kotlin for geometry checks. */
object MeshGeometry {
    fun sphere(slices: Int = 14, stacks: Int = 10): FloatArray {
        require(slices >= 3 && stacks >= 2)
        val out = ArrayList<Float>()
        fun point(i: Int, j: Int): FloatArray {
            val a = PI * i / stacks; val b = 2 * PI * j / slices
            return floatArrayOf((sin(a) * cos(b)).toFloat(), cos(a).toFloat(), (sin(a) * sin(b)).toFloat())
        }
        for (i in 0 until stacks) for (j in 0 until slices) {
            triangle(out, point(i,j), point(i,j+1), point(i+1,j))
            triangle(out, point(i,j+1), point(i+1,j+1), point(i+1,j))
        }
        return out.toFloatArray()
    }
    fun cone(slices: Int = 5): FloatArray {
        require(slices >= 3)
        val out = ArrayList<Float>()
        for (i in 0 until slices) {
            val a = 2 * PI * i / slices; val b = 2 * PI * (i+1) / slices
            val p = floatArrayOf(cos(a).toFloat(), -1f, sin(a).toFloat())
            val q = floatArrayOf(cos(b).toFloat(), -1f, sin(b).toFloat())
            triangle(out, floatArrayOf(0f,1f,0f), q, p)
            triangle(out, floatArrayOf(0f,-1f,0f), p, q)
        }
        return out.toFloatArray()
    }
    fun ring(): FloatArray {
        val out = ArrayList<Float>()
        fun point(i: Int, r: Float) = floatArrayOf(cos(i*2*PI/64).toFloat()*r, .002f, sin(i*2*PI/64).toFloat()*r)
        for(i in 0 until 64) {
            triangle(out, point(i,.92f), point(i+1,.92f), point(i,1f))
            triangle(out, point(i+1,.92f), point(i+1,1f), point(i,1f))
        }
        return out.toFloatArray()
    }
    private fun triangle(out: MutableList<Float>, a: FloatArray, b: FloatArray, c: FloatArray) {
        val u = FloatArray(3) { b[it]-a[it] }; val v = FloatArray(3) { c[it]-a[it] }
        val n = floatArrayOf(u[1]*v[2]-u[2]*v[1], u[2]*v[0]-u[0]*v[2], u[0]*v[1]-u[1]*v[0])
        val length = sqrt(n.sumOf { (it*it).toDouble() }).toFloat()
        if(length < 1e-7f) return
        for(p in listOf(a,b,c)) { out.addAll(p.toList()); out.addAll(n.map { it/length }) }
    }
}
