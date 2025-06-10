package mega.privacy.android.app.fragments.homepage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.model.SortByHeaderState
import mega.privacy.android.app.fragments.homepage.model.SortOrderState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCameraSortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetOfflineSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.SetCameraSortOrder
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.SetOfflineSortOrder
import mega.privacy.android.domain.usecase.SetOthersSortOrder
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import javax.inject.Inject

/**
 * ViewModel in charge of manage actions from sub-headers in which the view type and
 * sort by options can be changed.
 */
@HiltViewModel
class SortByHeaderViewModel @Inject constructor(
    private val getCameraSortOrder: GetCameraSortOrder,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getOthersSortOrder: GetOthersSortOrder,
    private val getOfflineSortOrder: GetOfflineSortOrder,
    private val monitorViewType: MonitorViewType,
    private val setCameraSortOrder: SetCameraSortOrder,
    private val setCloudSortOrder: SetCloudSortOrder,
    private val setOthersSortOrder: SetOthersSortOrder,
    private val setOfflineSortOrder: SetOfflineSortOrder,
    private val setViewType: SetViewType,
) : ViewModel() {

    private val _state = MutableStateFlow(SortByHeaderState())

    /**
     * Sort By Header State
     */
    val state: StateFlow<SortByHeaderState> = _state

    private val ordersDefault = SortOrder.ORDER_DEFAULT_ASC
    private val cameraOrderDefault = SortOrder.ORDER_MODIFICATION_DESC

    private val _cameraSortOrder = MutableStateFlow(cameraOrderDefault)

    /**
     * Camera Sort Order
     */
    private val cameraSortOrder: StateFlow<SortOrder> = _cameraSortOrder

    private val _cloudSortOrder = MutableStateFlow(ordersDefault)

    /**
     * Cloud Sort Order
     */
    val cloudSortOrder: StateFlow<SortOrder> = _cloudSortOrder

    private val _othersSortOrder = MutableStateFlow(ordersDefault)

    /**
     * Others Sort Order
     */
    val othersSortOrder: StateFlow<SortOrder> = _othersSortOrder

    private val _offlineSortOrder = MutableStateFlow(ordersDefault)

    /**
     * Offline Sort Order
     */
    val offlineSortOrder: StateFlow<SortOrder> = _offlineSortOrder

    private val _showDialogEvent = MutableLiveData<Event<Unit>>()

    /**
     * Show Dialog Event
     */
    val showDialogEvent: LiveData<Event<Unit>> = _showDialogEvent

    /**
     * Checks whether the current View Type is [ViewType.LIST] or not
     *
     * @return True if the current View Type is [ViewType.LIST]
     */
    fun isListView() = getStateViewType() == ViewType.LIST

    /**
     * The [ViewType] from [SortByHeaderState]
     *
     * @return [ViewType]
     */
    private fun getStateViewType() = _state.value.viewType

    /**
     * SortOrderState
     */
    var order = SortOrderState(
        cloudSortOrder = _cloudSortOrder.value,
        othersSortOrder = _othersSortOrder.value,
        offlineSortOrder = _offlineSortOrder.value
    )
        private set

    private val _orderChangeState = MutableSharedFlow<SortOrderState>()

    /**
     * Order changed state
     */
    val orderChangeState: SharedFlow<SortOrderState> = _orderChangeState

    private val _oldOrder = MutableStateFlow<SortOrder?>(null)

    init {
        refreshData()
        viewModelScope.launch {
            monitorViewType().collect { viewType ->
                _state.update { it.copy(viewType = viewType) }
            }
        }
    }

    internal fun refreshData(isUpdatedOrderChangeState: Boolean = false) {
        viewModelScope.launch {
            _cameraSortOrder.value = getCameraSortOrder()
            _cloudSortOrder.value = getCloudSortOrder()
            _othersSortOrder.value = getOthersSortOrder()
            _offlineSortOrder.value = getOfflineSortOrder()
            order = SortOrderState(
                cloudSortOrder = _cloudSortOrder.value,
                othersSortOrder = _othersSortOrder.value,
                offlineSortOrder = _offlineSortOrder.value
            )
            setOldOrder()
            if (isUpdatedOrderChangeState) {
                _orderChangeState.emit(order)
            }
        }
    }

    /**
     * Previously Selected Order
     */
    val oldOrder: StateFlow<SortOrder?> = _oldOrder

    private var orderType: Int = Constants.ORDER_CLOUD

    /**
     * Set Order Type
     */
    fun setOrderType(orderType: Int) {
        this.orderType = orderType
    }

    internal fun resetOlderOrder() {
        _oldOrder.value = null
    }

    /**
     * Set Old Order
     */
    private fun setOldOrder() {
        val order = when (this.orderType) {
            Constants.ORDER_CLOUD -> cloudSortOrder.value
            Constants.ORDER_CAMERA -> cameraSortOrder.value
            Constants.ORDER_OTHERS -> othersSortOrder.value
            Constants.ORDER_OFFLINE -> offlineSortOrder.value
            Constants.ORDER_FAVOURITES -> cloudSortOrder.value
            Constants.ORDER_VIDEO_PLAYLIST -> cloudSortOrder.value
            Constants.ORDER_OUTGOING_SHARES -> cloudSortOrder.value
            else -> SortOrder.ORDER_DEFAULT_ASC
        }
        _oldOrder.value = order
    }

    /**
     * Set Camera Sort Order
     */
    suspend fun setOrderCamera(order: SortOrder) = viewModelScope.launch {
        _cameraSortOrder.value = order
        setCameraSortOrder(order)
    }

    /**
     * Set Cloud Sort Order
     */
    suspend fun setOrderCloud(order: SortOrder) = viewModelScope.launch {
        _cloudSortOrder.value = order
        setCloudSortOrder(order)
    }

    /**
     * Set Others Sort Order
     */
    suspend fun setOrderOthers(order: SortOrder) = viewModelScope.launch {
        _othersSortOrder.value = order
        setOthersSortOrder(order)
    }

    /**
     * Set Offline Sort Order
     */
    suspend fun setOrderOffline(order: SortOrder) = viewModelScope.launch {
        _offlineSortOrder.value = order
        setOfflineSortOrder(order)
    }

    /**
     * Show Sort by Dialog
     */
    fun showSortByDialog() {
        _showDialogEvent.value = Event(Unit)
    }

    /**
     * Switches to a different View type
     */
    fun switchViewType() {
        viewModelScope.launch {
            when (getStateViewType()) {
                ViewType.LIST -> setViewType(ViewType.GRID)
                ViewType.GRID -> setViewType(ViewType.LIST)
            }
        }
    }

    internal fun updateWhenOrderChanged(newOrder: SortOrderState) =
        viewModelScope.launch {
            order = newOrder
            _orderChangeState.emit(newOrder)
        }

    companion object {
        /**
         * SortOrder to Display Name Map
         */
        @JvmStatic
        val orderNameMap = hashMapOf(
            SortOrder.ORDER_NONE to R.string.sortby_name,
            SortOrder.ORDER_DEFAULT_ASC to R.string.sortby_name,
            SortOrder.ORDER_DEFAULT_DESC to R.string.sortby_name,
            SortOrder.ORDER_MODIFICATION_ASC to R.string.sortby_date,
            SortOrder.ORDER_MODIFICATION_DESC to R.string.sortby_date,
            SortOrder.ORDER_LINK_CREATION_ASC to R.string.sortby_date,
            SortOrder.ORDER_LINK_CREATION_DESC to R.string.sortby_date,
            SortOrder.ORDER_SIZE_ASC to R.string.sortby_size,
            SortOrder.ORDER_SIZE_DESC to R.string.sortby_size,
            SortOrder.ORDER_FAV_ASC to R.string.file_properties_favourite,
            SortOrder.ORDER_LABEL_ASC to R.string.title_label
        )
    }
}
