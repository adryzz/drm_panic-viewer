package gay.nihil.lena.drm_panic_viewer

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PanicDao {
    @Insert
    fun insert(event: PanicMessage)

    @Query("SELECT * FROM PanicMessage ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEventsLive(limit: Int): LiveData<MutableList<PanicMessage>>

    @Query("SELECT * FROM PanicMessage ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int): MutableList<PanicMessage>
}