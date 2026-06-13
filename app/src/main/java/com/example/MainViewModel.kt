package com.example

import android.app.Application
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredApps: StateFlow<List<AppInfo>> = combine(_apps, _searchQuery) { apps, query ->
        if (query.isBlank()) {
            apps
        } else {
            apps.filter { it.name.contains(query, ignoreCase = true) }
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val packageManager = getApplication<Application>().packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = packageManager.queryIntentActivities(intent, 0)
            
            val appList = resolveInfos.mapNotNull { info ->
                try {
                    val name = info.loadLabel(packageManager).toString()
                    val packageName = info.activityInfo.packageName
                    val icon = info.loadIcon(packageManager)
                    AppInfo(name = name, packageName = packageName, icon = icon)
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.name.lowercase() }
            
            _apps.value = appList
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun clearSearchQuery() {
        _searchQuery.value = ""
    }
}
