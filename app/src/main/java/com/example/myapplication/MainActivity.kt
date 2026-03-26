package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.myapplication.data.provideRepository
import com.example.myapplication.model.DramaItem
import com.example.myapplication.ui.ErrorDialog
import com.example.myapplication.ui.FreeReelsViewModel
import com.example.myapplication.ui.FreeReelsViewModelFactory
import com.example.myapplication.ui.ScreenTab
import com.example.myapplication.ui.theme.FreeReelsTheme
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.myapplication.utils.ErrorLogger

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<FreeReelsViewModel> {
        FreeReelsViewModelFactory(provideRepository())
    }
    
    private var errorDetail by mutableStateOf<ErrorLogger.ErrorDetail?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize error logger
        ErrorLogger.init(this) { error ->
            errorDetail = error
        }
        
        setContent {
            FreeReelsTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    FreeReelsApp(viewModel)
                }
                
                // Show error dialog if there's an error
                errorDetail?.let { error ->
                    ErrorDialog(
                        errorDetail = error,
                        onDismiss = { errorDetail = null }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FreeReelsApp(viewModel: FreeReelsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeFeed = uiState.feeds.getValue(uiState.activeTab)
    val hero = activeFeed.items.firstOrNull()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = " NxDrama",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Text(
                text = "Nonton Drama dan Anime",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(10.dp))

            HeroSection(
                item = hero,
                onWatchClick = { hero?.let(viewModel::openDetail) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(
                selectedTabIndex = uiState.activeTab.ordinal,
                containerColor = Color.Transparent,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                ScreenTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.activeTab == tab,
                        onClick = { viewModel.onTabSelected(tab) },
                        text = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }

            if (uiState.activeTab == ScreenTab.SEARCH) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        singleLine = true,
                        placeholder = { Text("Cari judul drama atau anime") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = viewModel::performSearch) {
                        Text("Cari")
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    activeFeed.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    activeFeed.error != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "⚠️ ${activeFeed.error ?: "Terjadi kesalahan"}",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp),
                            )
                            Button(
                                onClick = {
                                    if (uiState.activeTab == ScreenTab.SEARCH) {
                                        viewModel.performSearch()
                                    } else {
                                        viewModel.onTabSelected(uiState.activeTab)
                                    }
                                }
                            ) {
                                Text("Coba Lagi")
                            }
                        }
                    }

                    activeFeed.items.isEmpty() -> {
                        Text(
                            text = "Belum ada data untuk ditampilkan.",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                        )
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(activeFeed.items, key = { it.id }) { item ->
                                DramaRow(item = item, onClick = { viewModel.openDetail(item) })
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.selectedItem?.let { item ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDetail,
            title = { Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.description, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (item.videoUrl.isBlank()) {
                        Text(
                            text = "Video belum tersedia untuk judul ini.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        NativeVideoPlayer(url = item.videoUrl)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissDetail) {
                    Text("Tutup")
                }
            },
        )
    }
}

@Composable
private fun HeroSection(item: DramaItem?, onWatchClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF101317))
    ) {
        if (item == null) {
            Text(
                text = "Sedang menyiapkan rekomendasi...",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                color = Color(0xFFB0BEC5),
            )
        } else {
            AsyncImage(
                model = item.cover,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = item.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = item.description,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(onClick = onWatchClick) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        androidx.compose.material3.Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text("Tonton")
                    }
                }
            }
        }
    }
}

@Composable
private fun DramaRow(item: DramaItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AsyncImage(
            model = item.cover,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 70.dp, height = 98.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${item.episodes} episode • ${item.followers} followers • ${item.subtitleCount} subtitle",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
            )
        }
    }
}

@Composable
private fun NativeVideoPlayer(url: String) {
    val context = LocalContext.current
    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(10.dp))
    )
}