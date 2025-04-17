package gay.nihil.lena.drm_panic_viewer

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import gay.nihil.lena.drm_panic_viewer.UriConverters

@Database(entities = [PanicMessage::class], version = 2)
@TypeConverters(UriConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun panicDao(): PanicDao?
}