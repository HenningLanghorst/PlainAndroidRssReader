package de.henninglanghorst.rssreader

import android.arch.persistence.room.*
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.content.Context

@Entity(tableName = "FEED")
data class Feed(
        @PrimaryKey(autoGenerate = true) var id: Long? = null,
        @ColumnInfo(name = "url") var url: String = ""
)


@Dao
interface FeedDao {

    @Query("SELECT * from FEED")
    fun getAll(): List<Feed>

    @Insert(onConflict = REPLACE)
    fun insert(feed: Feed)

}


@Database(entities = arrayOf(Feed::class), version = 1)
abstract class FeedDatabase : RoomDatabase() {

    abstract val feedDao: FeedDao

}