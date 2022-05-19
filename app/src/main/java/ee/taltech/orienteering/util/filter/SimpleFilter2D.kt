package ee.taltech.orienteering.util.filter

import ee.taltech.orienteering.util.datatype.Vector2D

class SimpleFilter2D(private val length: Int): IFilter<Vector2D> {

    private val data = mutableListOf<Vector2D>()

    override fun process(input: Vector2D): Vector2D {
        if (data.size >= length) {
            data.removeAt(0)
        }
        data.add(input)
        val avg = Vector2D(0.0, 0.0)
        for (item in data) {
            avg.add(item)
        }
        return avg.divide(length.toDouble()) // Be conservative on boot
    }

}