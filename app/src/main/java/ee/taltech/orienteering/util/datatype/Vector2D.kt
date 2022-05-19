package ee.taltech.orienteering.util.datatype

import kotlin.math.sqrt

class Vector2D(var x: Double, var y: Double) {
    fun add(other: Vector2D): Vector2D {
        x += other.x
        y += other.y
        return this
    }

    fun divide(n: Double): Vector2D {
        x /= n
        y /= n
        return this
    }

    fun dot(): Double {
        return x * x + y * y
    }

    fun length(): Double {
        return sqrt(dot())
    }
}