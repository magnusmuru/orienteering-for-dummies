package ee.taltech.orienteering.util

import java.security.MessageDigest
import java.util.*

class HashUtils {

    companion object {
        val HEX_CHARS = "0123456789ABCDEF".toCharArray()

        fun sha1(input: String) = hashString("SHA-1", input)
        fun md5(input: String) = hashString("MD5", input)

        private fun hashString(type: String, input: String): String {
            val bytes = MessageDigest
                .getInstance(type)
                .digest(input.toByteArray())
            return getHexBinaryString(bytes).toLowerCase(Locale.ROOT)
        }

        private fun getHexBinaryString(data: ByteArray): String {
            val r = StringBuilder(data.size * 2)
            data.forEach { b ->
                val i = b.toInt()
                r.append(HEX_CHARS[i shr 4 and 0xF])
                r.append(HEX_CHARS[i and 0xF])
            }
            return r.toString()
        }
    }
}