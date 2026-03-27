package com.example.myapplication.ui

import android.os.Bundle
import android.os.Build
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.myapplication.model.DramaItem
import com.example.myapplication.utils.ErrorLogger

class DetailActivity : ComponentActivity() {
    companion object {
        const val EXTRA_DRAMA_ITEM = "drama_item"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val dramaItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getSerializableExtra(EXTRA_DRAMA_ITEM, DramaItem::class.java)
        } else {
        @Suppress("DEPRECATION")
        intent.getSerializableExtra(EXTRA_DRAMA_ITEM) as? DramaItem
    }
        
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (dramaItem != null) {
                        DetailScreen(
                            item = dramaItem,
                            onBackPressed = { finish() }
                        )
                    } else {
                        Text("Data tidak ditemukan")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(item: DramaItem, onBackPressed: () -> Unit) {
    var showVideoPlayer by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = item.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 48.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Video Player Section - Sticky Header
            stickyHeader {
                if (item.videoUrl.isNotBlank() && showVideoPlayer) {
                    NativeVideoPlayerFull(
                        url = item.videoUrl,
                        onError = { error ->
                            ErrorLogger.logVideoError(
                                errorMessage = error,
                                videoUrl = item.videoUrl,
                                dramaTitle = item.title,
                                dramaId = item.id
                            )
                        }
                    )
                } else if (item.videoUrl.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Video belum tersedia",
                            color = Color.White
                        )
                    }
                }
            }
            
            // Episode List Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Episodes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Episode Items
            items(generateEpisodes(item.episodes)) { episode ->
                EpisodeRow(
                    episodeNumber = episode,
                    onClick = { 
                        // Handle episode click - you can implement episode-specific video URLs here
                        // For now, just show a message or play the same video
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun NativeVideoPlayerFull(url: String, onError: (String) -> Unit = {}) {
    val context = LocalContext.current
    var playerError by remember { mutableStateOf<String?>(null) }
    var isPlayerReady by remember { mutableStateOf(false) }
    
    // Gunakan remember dengan key yang stabil agar player tidak direcreate saat recomposition
    val exoPlayer = remember(url) {
        try {
            ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        super.onPlayerError(error)
                        val errorMessage = "Video playback error: ${error.message}"
                        playerError = errorMessage
                        onError(errorMessage)
                    }
                    
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        isPlayerReady = playbackState == Player.STATE_READY
                    }
                })
                
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = true
            }
        } catch (e: Exception) {
            val errorMessage = "Failed to initialize player: ${e.message}"
            playerError = errorMessage
            onError(errorMessage)
            null
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            // Jangan release player saat dispose untuk menjaga state video
            // Player akan tetap berjalan meskipun composable di-recompose
            // exoPlayer?.release()
        }
    }

    if (playerError != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚠️ $playerError",
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else if (exoPlayer != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            if (!isPlayerReady) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
fun EpisodeRow(episodeNumber: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Column {
                Text(
                    text = "Episode $episodeNumber",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Tonton episode ini",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        Text(
            text = "▶",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun generateEpisodes(count: Int): List<Int> = (1..count).toList()