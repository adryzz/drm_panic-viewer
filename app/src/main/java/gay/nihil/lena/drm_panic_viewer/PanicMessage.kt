package gay.nihil.lena.drm_panic_viewer

import android.net.Uri
import android.os.Parcelable
import androidx.core.net.toUri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.time.Instant
import java.util.zip.InflaterInputStream
import kotlin.streams.asStream

@Entity
@Parcelize
class PanicMessage(

    @JvmField
    @ColumnInfo(name = "log")
    var log: String? = null,

    @JvmField
    @ColumnInfo(name = "reason")
    var reason: String? = null,

    @JvmField
    @ColumnInfo(name = "hardware_name")
    var hardwareName: String? = null,

    @JvmField
    @ColumnInfo(name = "architecture")
    var architecture: String? = null,

    @JvmField
    @ColumnInfo(name = "version")
    var version: String? = null,

    @JvmField
    @ColumnInfo(name = "distribution")
    var distribution: String? = null,

    @JvmField
    @ColumnInfo(name = "report_uri")
    var reportUri: Uri? = null,

    @JvmField
    @ColumnInfo(name = "log_type")
    var logType: Int = PanicMessageType.RAW,

    @JvmField
    @PrimaryKey
    @ColumnInfo(name = "timestamp")
    var timestamp: Long = 0) : Parcelable {

    object PanicMessageType {
        const val RAW: Int = 0
        const val LEGACY: Int = 1
        const val FULL: Int = 2
    }

    companion object {
        @JvmStatic
        fun parse(uri: Uri) : PanicMessage {
            var p = PanicMessage()
            p.timestamp = Instant.now().epochSecond
            if (uri.host == null) {
                // raw kernel panic
                p.log = uri.toString()

                p.log!!.lineSequence().asStream().forEach { l: String? ->
                    // TODO: make this faster and abstract out
                    if (l!!.contains("Kernel panic - not syncing:")) {
                        p.reason = l.substring(l.indexOf(':') + 2)
                    }
                    if (l.contains("Hardware name:")) {
                        p.hardwareName = l.substring(l.indexOf(':') + 2)
                    }
                }
            } else {

                p.reportUri = uri
                var stuff = uri
                if (uri.fragment != null) {
                    // god fuck the Java URI API
                    stuff = uri.encodedFragment!!.toUri()
                }

                p.architecture = stuff.getQueryParameter("a")
                p.version = stuff.getQueryParameter("v")
                p.distribution = stuff.getQueryParameter("d")

                if (p.distribution == null) {
                    // TODO: make a list of names
                    p.distribution = p.reportUri!!.host
                }

                // decode encoded log

                val zlibbed = if (stuff.getQueryParameter("z") != null) {
                    p.logType = PanicMessageType.FULL
                    numbersToData2(stuff.getQueryParameter("z")!!)
                } else {
                    // legacy encoding, v6.10 to v6.13
                    p.logType = PanicMessageType.LEGACY
                    numbersToData(stuff.getQueryParameter("zl")!!)
                }

                p.log = InflaterInputStream(zlibbed.inputStream()).bufferedReader().use { it.readText() }

                p.log!!.lineSequence().asStream().forEach { l: String? ->
                    // TODO: make this faster and abstract out
                    if (l!!.contains("Kernel panic - not syncing:")) {
                        p.reason = l.substring(l.indexOf(':') + 2)
                    }
                    if (l.contains("Hardware name:")) {
                        p.hardwareName = l.substring(l.indexOf(':') + 2)
                    }
                }
            }
            return p
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
}