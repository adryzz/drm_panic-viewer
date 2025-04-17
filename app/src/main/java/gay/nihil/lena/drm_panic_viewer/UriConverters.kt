package gay.nihil.lena.drm_panic_viewer

import android.net.Uri
import androidx.room.TypeConverter
import androidx.core.net.toUri

class UriConverters {
    @TypeConverter
    fun fromString(value: String?): Uri? {
        return if (value == null) null else value.toUri()
    }

    @TypeConverter
    fun toString(uri: Uri?): String? {
        return uri?.toString()
    }
}