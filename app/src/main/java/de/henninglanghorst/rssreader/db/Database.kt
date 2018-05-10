package de.henninglanghorst.rssreader.db

import android.arch.persistence.room.*
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.content.Context
import io.reactivex.Flowable

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

    @Delete
    fun delete(feed: Feed)
}


@Database(entities = [(Feed::class)], version = 1)
abstract class FeedDatabase : RoomDatabase() {

    abstract val feedDao: FeedDao

    companion object {

        private var instance: FeedDatabase? = null

        operator fun invoke(applicationContext: Context): FeedDatabase =
                synchronized(this) {
                    if (instance == null) {
                        instance = Room.databaseBuilder(applicationContext, FeedDatabase::class.java, "feed.db").build()
                    }
                    return instance!!
                }
    }

}