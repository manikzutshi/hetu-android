package com.aurafarmers.hetu.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurafarmers.hetu.data.local.dao.FeedPostDao
import com.aurafarmers.hetu.data.local.entity.FeedPostEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedPostDao: FeedPostDao
) : ViewModel() {

    val feedPosts: StateFlow<List<FeedPostEntity>> = feedPostDao.getAllPosts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addPost(mediaUri: String, caption: String?, location: String?, mediaType: String, category: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            feedPostDao.insert(
                FeedPostEntity(
                    mediaUri = mediaUri,
                    caption = caption,
                    location = location,
                    timestamp = timestamp,
                    mediaType = mediaType,
                    category = category
                )
            )
        }
    }

    fun deletePost(post: FeedPostEntity) {
        viewModelScope.launch {
            feedPostDao.delete(post)
        }
    }
}
