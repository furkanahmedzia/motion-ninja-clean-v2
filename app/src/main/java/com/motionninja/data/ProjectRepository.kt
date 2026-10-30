package com.motionninja.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import com.motionninja.data.converters.ExportConfigConverter
import com.motionninja.data.converters.TrackListConverter
import com.motionninja.domain.ExportConfig
import com.motionninja.domain.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asLiveData
import java.util.concurrent.Executors

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val durationUs: Long = 0,
    @TypeConverters(TrackListConverter::class) val tracksJson: String = "[]",
    val canvasWidth: Int = 1080,
    val canvasHeight: Int = 1920,
    val fps: Int = 30,
    @TypeConverters(ExportConfigConverter::class) val exportConfig: ExportConfig? = null
)

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity): Long
    
    @Update
    suspend fun update(project: ProjectEntity): Int
    
    @Delete
    suspend fun delete(project: ProjectEntity): Int
    
    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long): Int
    
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): LiveData<List<ProjectEntity>>
    
    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?
    
    @Query("UPDATE projects SET updatedAt = :now WHERE id = :id")
    suspend fun touchProject(id: Long, now: Long): Int
}

@Database(
    entities = [ProjectEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(ExportConfigConverter::class, TrackListConverter::class)
abstract class MotionDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    
    companion object {
        @Volatile private var INSTANCE: MotionDatabase? = null
        
        fun getInstance(context: Context): MotionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MotionDatabase::class.java,
                    "motion_ninja.db"
                ).fallbackToDestructiveMigration()
                    .setQueryExecutor(Executors.newSingleThreadExecutor())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ProjectRepository(private val dao: ProjectDao) {
    
    val allProjects: LiveData<List<Project>> = dao.getAllProjects().map { entities ->
        entities.map { entityToDomain(it) }
    }
    
    suspend fun createProject(name: String): Long {
        val entity = ProjectEntity(name = name)
        return dao.insert(entity)
    }
    
    suspend fun saveProject(project: Project) {
        val entity = domainToEntity(project)
        if (project.id > 0) {
            dao.update(entity)
        } else {
            val newId = dao.insert(entity)
            // Project ID updated via entity reference
        }
    }
    
    suspend fun deleteProject(id: Long) {
        dao.deleteById(id)
    }
    
    suspend fun getProject(id: Long): Project? {
        val entity = dao.getProjectById(id)
        return entity?.let { entityToDomain(it) }
    }
    
    fun observeProjects(): LiveData<List<Project>> = allProjects
    
    private fun entityToDomain(entity: ProjectEntity): Project {
        // TODO: Deserialize tracksJson, exportConfig
        return Project(
            id = entity.id,
            name = entity.name,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            thumbnailPath = entity.thumbnailPath,
            durationUs = entity.durationUs,
            tracks = emptyList(),
            canvasWidth = entity.canvasWidth,
            canvasHeight = entity.canvasHeight,
            fps = entity.fps
        )
    }
    
    private fun domainToEntity(project: Project): ProjectEntity {
        return ProjectEntity(
            id = project.id,
            name = project.name,
            createdAt = project.createdAt,
            updatedAt = project.updatedAt,
            thumbnailPath = project.thumbnailPath,
            durationUs = project.durationUs,
            tracksJson = "[]", // TODO: serialize
            canvasWidth = project.canvasWidth,
            canvasHeight = project.canvasHeight,
            fps = project.fps,
            exportConfig = project.exportConfig
        )
    }
}