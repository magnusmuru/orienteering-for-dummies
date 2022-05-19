package ee.taltech.orienteering.util.filter

interface IFilter<T> {

    fun process(input: T): T
}