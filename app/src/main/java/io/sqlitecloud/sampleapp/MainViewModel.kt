package io.sqlitecloud.sampleapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.serialization.json.Json

class MainViewModel : ViewModel() {
    private val _textData = MutableLiveData<String>()
    val textData: LiveData<String> get() = _textData

    init {
        updateText("connecting...")
    }

    fun updateText(newText: String) {
        _textData.value = newText
    }
}
