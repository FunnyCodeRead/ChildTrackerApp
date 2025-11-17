package com.example.childtrackerapp.schedule.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childtrackerapp.data.repository.ScheduleRepository
import com.example.childtrackerapp.schedule.model.Schedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AddScheduleUiState(
    val date: String = "",
    val title: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val description: String = "",
    val location: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddScheduleViewModel @Inject constructor(
    private val repository: ScheduleRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddScheduleUiState())
    val uiState: StateFlow<AddScheduleUiState> = _uiState.asStateFlow()
    
    fun setDate(date: String) {
        _uiState.update { it.copy(date = date) }
    }
    
    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, error = null) }
    }
    
    fun updateStartTime(time: String) {
        _uiState.update { it.copy(startTime = time, error = null) }
    }
    
    fun updateEndTime(time: String) {
        _uiState.update { it.copy(endTime = time, error = null) }
    }
    
    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }
    
    fun updateLocation(location: String) {
        _uiState.update { it.copy(location = location) }
    }
    
    fun saveSchedule() {
        val state = _uiState.value
        
        // Validation
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "Vui lòng nhập tiêu đề") }
            return
        }
        
        if (state.startTime.isBlank()) {
            _uiState.update { it.copy(error = "Vui lòng nhập thời gian bắt đầu") }
            return
        }
        
        if (state.endTime.isBlank()) {
            _uiState.update { it.copy(error = "Vui lòng nhập thời gian kết thúc") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val schedule = Schedule(
                    id = UUID.randomUUID().toString(),
                    title = state.title,
                    startTime = state.startTime,
                    endTime = state.endTime,
                    description = state.description,
                    location = state.location,
                    date = state.date
                )
                
                repository.addSchedule(schedule)
                
                _uiState.update { 
                    it.copy(isLoading = false, isSaved = true) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "Lỗi khi lưu lịch trình: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun importFromExcel() {
        // TODO: Implement Excel import functionality
        _uiState.update { 
            it.copy(error = "Tính năng nhập từ file Excel đang được phát triển") 
        }
    }
}
