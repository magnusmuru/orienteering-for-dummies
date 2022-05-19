package ee.taltech.orienteering.util.filter

class SimpleFilter(val length: Int): IFilter<Double> {

    private val data = mutableListOf<Double>()

    override fun process(input: Double): Double {
        if (data.size >= length) {
            data.removeAt(0)
        }
        data.add(input)
        var avg = 0.0;
        for (item in data) {
            avg += item
        }
        return avg / data.size
    }
}