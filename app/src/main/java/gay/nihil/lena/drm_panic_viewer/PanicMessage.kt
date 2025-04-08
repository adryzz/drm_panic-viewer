package gay.nihil.lena.drm_panic_viewer

import android.net.Uri
import java.util.zip.InflaterInputStream
import kotlin.streams.asStream
import androidx.core.net.toUri

class PanicMessage(uri: Uri) {
    var log: String? = null
    @JvmField
    var reason: String? = null
    var hardwareName: String? = null
    var architecture: String? = null
    var version: String? = null
    var distribution: String? = null
    @JvmField
    var reportUri: Uri? = null

    init {
        if (uri.host == null) {
            // raw kernel panic
            this.log = uri.toString()

            log!!.lineSequence().asStream().forEach { l: String? ->
                // TODO: make this faster and abstract out
                if (l!!.contains("Kernel panic - not syncing:")) {
                    this.reason = l.substring(l.indexOf(':') + 2)
                }
                if (l.contains("Hardware name:")) {
                    this.hardwareName = l.substring(l.indexOf(':') + 2)
                }
            }
        } else {
            // TODO: fix

            this.reportUri = uri
            var stuff = uri
            if (uri.fragment != null) {
                // god fuck the Java URI API
                stuff = uri.encodedFragment!!.toUri()
            }

            this.architecture = stuff.getQueryParameter("a")
            this.version = stuff.getQueryParameter("v")
            this.distribution = stuff.getQueryParameter("d")

            if (distribution == null) {
                // TODO: make a list of names
                this.distribution = reportUri!!.host
            }

            // decode encoded log

            val zlibbed = if (stuff.getQueryParameter("z") != null) {
                numbersToData2(stuff.getQueryParameter("z")!!)
            } else {
                // legacy encoding, v6.10 to v6.13
                numbersToData(stuff.getQueryParameter("zl")!!)
            }

            log = InflaterInputStream(zlibbed.inputStream()).bufferedReader().use { it.readText() }

            log!!.lineSequence().asStream().forEach { l: String? ->
                // TODO: make this faster and abstract out
                if (l!!.contains("Kernel panic - not syncing:")) {
                    this.reason = l.substring(l.indexOf(':') + 2)
                }
                if (l.contains("Hardware name:")) {
                    this.hardwareName = l.substring(l.indexOf(':') + 2)
                }
            }
        }
    }

    // ported directly from https://github.com/kdj0c/panic_report
    // under MIT license (c) 2024 Jocelyn Falempe
    fun numbersToData2(numbers: String): ByteArray {
        // 17 decimal digits are converted to 7 bytes
        val mainLen = (numbers.length / 17) * 7
        // And the remaining bytes, compute the reverse of [0, 3, 5, 8, 10, 13, 15, 17]
        val remLen = ((numbers.length % 17) * 2) / 5

        val data = ByteArray(mainLen + remLen)
        var offset = 0

        for (i in numbers.indices step 17) {
            val chunk = numbers.substring(i, (i + 17).coerceAtMost(numbers.length))
            var num = chunk.toBigInteger()
            var numBytes = 7
            if (chunk.length < 17) {
                numBytes = remLen
            }

            for (j in 0 until numBytes) {
                data[offset] = (num % 256.toBigInteger()).toByte()
                offset++
                num /= 256.toBigInteger()
            }
        }
        return data
    }

    // ported directly from https://github.com/kdj0c/panic_report
    // under MIT license (c) 2024 Jocelyn Falempe
    fun numbersToData(numbers: String): ByteArray {
        val numCharsToBits = intArrayOf(0, 4, 7, 10, 13)
        val lengthInBits = (numbers.length / 4) * 13 + numCharsToBits[numbers.length % 4]
        val lengthInBytes = lengthInBits / 8
        val extra = lengthInBits % 8
        val data = ByteArray(lengthInBytes)

        var offset = 0
        var byteOff = 0
        var rem = 0

        for (i in numbers.indices step 4) {
            val chunk = numbers.substring(i, (i + 4).coerceAtMost(numbers.length))
            val num = chunk.toInt()
            val newLength = numCharsToBits[chunk.length]

            var b = offset + newLength
            if (byteOff * 8 + b >= lengthInBytes * 8) {
                b -= extra
            }
            if (b < 8) {
                rem += num shl (8 - b)
                offset = b
            } else if (b < 16) {
                data[byteOff] = (rem + (num shr (b - 8))).toByte()
                byteOff++
                rem = (num shl (16 - b)) and 0xFF
                offset = (b - 8)
            } else {
                data[byteOff] = (rem + (num shr (b - 8))).toByte()
                byteOff++
                data[byteOff] = (num shr (b - 16)).toByte()
                byteOff++
                rem = (num shl (24 - b)) and 0xFF
                offset = (b - 16)
            }
        }
        return data
    }
}