package de.henninglanghorst.rssreader.db

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


@Database(entities = [(Feed::class)], version = 1)
abstract class FeedDatabase : RoomDatabase() {

    abstract val feedDao: FeedDao

    companion object {
        operator fun invoke(applicationContext: Context) =
                Room.databaseBuilder(
                        applicationContext,
                        FeedDatabase::class.java,
                        "feed.db").build()
    }

}