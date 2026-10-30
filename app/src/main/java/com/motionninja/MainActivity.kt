package com.motionninja

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.motionninja.data.ProjectRepository
import com.motionninja.domain.Project
import com.motionninja.ui.theme.Theme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val projectRepository: ProjectRepository by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Theme { MotionNinjaNavHost(projectRepository) } }
    }
}

@Composable
fun MotionNinjaNavHost(projectRepository: ProjectRepository) {
    val navController = rememberNavController()
    var currentTab by remember { mutableStateOf(Tab.HOME) }
    
    NavHost(navController, startDestination = Tab.HOME.route) {
        composable(Tab.HOME.route) {
            HomeScreen(
                onNewProject = { currentTab = Tab.EDITOR; navController.navigate(Tab.EDITOR.route) },
                repository = projectRepository,
                navController = navController
            )
        }
        composable(
            route = "${Tab.EDITOR.route}?projectId={projectId}",
            arguments = listOf(androidx.navigation.navArgument("projectId") { type = androidx.navigation.NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.getLong() ?: 0L
            EditorActivity.ComposeEntryPoint(projectId = projectId)
        }
    }
    
    NavigationBar {
        Tab.values().forEach { tab ->
            NavigationBarItem(
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                selected = currentTab == tab,
                onClick = {
                    currentTab = tab
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

@Composable
fun HomeScreen(
    onNewProject: () -> Unit,
    repository: ProjectRepository,
    navController: NavController
) {
    val projects by repository.observeProjects()
    
    Theme.Surface {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Motion Ninja", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Theme.ColorScheme.surface)
            )
            
            if (projects.isEmpty()) {
                // Empty state
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_video_library), "No projects", size = 80.dp)
                        Text("No projects yet", style = Theme.Typography.headlineSmall)
                        Text("Create your first video project", style = Theme.Typography.bodyMedium, color = Theme.ColorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(projects) { project ->
                        ProjectCard(project) {
                            navController.navigate("${Tab.EDITOR.route}?projectId=${project.id}")
                        }
                    }
                }
            }
            
            FloatingActionButton(
                onClick = onNewProject,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) { Icon(androidx.compose.material.icons.defaults.Icons.Default.Add, "New Project") }
        }
    }
}

@Composable
fun ProjectCard(project: Project, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f/16f)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            // Thumbnail placeholder
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.dp)
                    .weight(1f)
                    .background(Theme.ColorScheme.surfaceContainerHighest)
            ) {
                project.thumbnailPath?.let { path ->
                    // TODO: Load thumbnail with Coil
                }
            }
            // Info overlay
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Text(project.name, maxLines = 1, overflow = androidx.compose.ui.text.TextOverflow.Ellipsis, 
                     style = Theme.Typography.titleSmall, fontWeight = FontWeight.Medium)
                Text("${project.durationUs / 1_000_000}s • ${project.tracks.size} tracks",
                     style = Theme.Typography.labelSmall, color = Theme.ColorScheme.onSurfaceVariant)
            }
        }
    }
}

enum class Tab(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String) {
    HOME("home", androidx.compose.material.icons.defaults.Icons.Default.Home, "Projects"),
    EDITOR("editor", androidx.compose.material.icons.defaults.Icons.Default.VideoCamera, "Editor")
    // REMOVED: PUBLIC, TEMPLATE, DISCOVER
}