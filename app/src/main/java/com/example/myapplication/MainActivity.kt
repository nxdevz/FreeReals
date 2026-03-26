package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.myapplication.data.provideRepository
import com.example.myapplication.model.DramaItem
import com.example.myapplication.FreeReelsViewModel
import com.example.myapplication.ui.FreeReelsViewModelFactory
import com.example.myapplication.ui.ScreenTab
import com.example.myapplication.ui.theme.FreeReelsTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<FreeReelsViewModel> {
        FreeReelsViewModelFactory(provideRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FreeReelsTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    FreeReelsApp(viewModel)
                }
            }
        }
    }
}

@Composable
private fun FreeReelsApp(viewModel: FreeReelsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeFeed = uiState.feeds.getValue(uiState.activeTab)
    val hero = activeFeed.items.firstOrNull()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderSection()

            AnimatedContent(
                targetState = hero?.id,
                label = "hero-transition",
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(220)) },
            ) {
                HeroSection(
                    item = hero,
                    onWatchClick = { hero?.let(viewModel::openDetail) },
                    onSearchClick = { viewModel.onTabSelected(ScreenTab.SEARCH) },
                )
            }

            TabBar(activeTab = uiState.activeTab, onSelect = viewModel::onTabSelected)

            AnimatedVisibility(
                visible = uiState.activeTab == ScreenTab.SEARCH,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(140)),
            ) {
                SearchSection(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onSearch = viewModel::performSearch,
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    activeFeed.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    activeFeed.error != null -> {
                        Text(
                            text = activeFeed.error ?: "Terjadi kesalahan",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 20.dp),
                        )
                    }

                    activeFeed.items.isEmpty() -> {
                        Text(
                            text = "Belum ada data untuk ditampilkan.",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            items(activeFeed.items, key = { it.id }) { item ->
                                DramaRow(item = item, onClick = { viewModel.openDetail(item) })
                                HorizontalDivider(color = Color(0xFF27272A), thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        }

        DetailOverlay(item = uiState.selectedItem, onDismiss = viewModel::dismissDetail)
    }
}

@Composable
private fun HeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = "FreeReels Mobile",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF6EE7B7),
        )
        Text(
            text = "Nonton Drama & Anime",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HeroSection(
    item: DramaItem?,
    onWatchClick: () -> Unit,
    onSearchClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(256.dp)
            .background(Color(0xFF18181B))
    ) {
        if (item == null) {
            Text(
                text = "Sedang menyiapkan rekomendasi untuk kamu...",
                color = Color(0xFFA1A1AA),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
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
                    .background(Color.Black.copy(alpha = 0.42f)),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onWatchClick) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Text("Tonton Sekarang")
                        }
                    }

                    TextButton(
                        onClick = onSearchClick,
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                        )
                    ) {
                        Text("Cari Judul", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabBar(activeTab: ScreenTab, onSelect: (ScreenTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ScreenTab.entries.forEach { tab ->
            val selected = activeTab == tab
            val bgAlpha by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = tween(260),
                label = "tab-bg",
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF6EE7B7).copy(alpha = bgAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.label,
                    maxLines = 1,
                    color = if (selected) Color(0xFF09090B) else Color(0xFFD4D4D8),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun SearchSection(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("Cari judul drama atau anime") },
        )
        Button(onClick = onSearch) {
            Text("Cari")
        }
    }
}

@Composable
private fun DramaRow(item: DramaItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AsyncImage(
            model = item.cover,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 64.dp, height = 96.dp)
                .clip(RoundedCornerShape(6.dp)),
        )

        Column(
            modifier = Modifier.weight(1f),
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
                text = "${item.episodes} episode • ${formatFollowers(item.followers)} followers • ${item.subtitleCount} subtitle",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
            )
        }
    }
}

@Composable
private fun DetailOverlay(item: DramaItem?, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = item != null,
        enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { it / 5 }),
        exit = fadeOut(tween(180)) + slideOutVertically(targetOffsetY = { it / 5 }),
    ) {
        val selectedItem = item ?: return@AnimatedVisibility

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color(0xFF18181B))
                    .padding(16.dp)
                    .imePadding()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {},
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.clickable(onClick = onDismiss),
                    )
                    Text(
                        text = selectedItem.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = selectedItem.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                if (selectedItem.videoUrl.isBlank()) {
                    Text(
                        text = "Video belum tersedia untuk judul ini.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    NativeVideoPlayer(url = selectedItem.videoUrl)
                }

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Tutup")
                }
            }
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
        onDispose { exoPlayer.release() }
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
            .clip(RoundedCornerShape(10.dp)),
    )
}

private fun formatFollowers(value: Long): String {
    if (value < 1_000) return value.toString()
    if (value < 1_000_000) {
        val compact = (value / 100) / 10f
        return "${compact.toString().trimEnd('0').trimEnd('.')}K"
    }

    val compact = (value / 100_000) / 10f
    return "${compact.toString().trimEnd('0').trimEnd('.')}M"
}