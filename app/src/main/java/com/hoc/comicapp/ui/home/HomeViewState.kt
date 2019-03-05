package com.hoc.comicapp.ui.home

import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.base.SingleEvent
import com.hoc.comicapp.base.ViewState
import com.hoc.comicapp.data.models.Comic
import com.hoc.comicapp.data.models.ComicAppError
import com.hoc.comicapp.data.models.getMessageFromError

sealed class HomeListItem {
  data class SuggestListState(
    val comics: List<Comic>,
    val errorMessage: String?,
    val isLoading: Boolean
  ) : HomeListItem()

  data class TopMonthListState(
    val comics: List<Comic>,
    val errorMessage: String?,
    val isLoading: Boolean
  ) : HomeListItem()

  sealed class UpdatedItem : HomeListItem() {
    data class ComicItem(val comic: Comic) : UpdatedItem()
    data class Error(val errorMessage: String?) : UpdatedItem()
    object Loading : UpdatedItem()
  }

  data class Header(val type: HeaderType) : HomeListItem()

  enum class HeaderType { SUGGEST, TOP_MONTH, UPDATED }
}

data class HomeViewState(
  val items: List<HomeListItem>,
  val refreshLoading: Boolean,
  val updatedPage: Int
) : ViewState {
  companion object {
    @JvmStatic
    fun initialState() = HomeViewState(
      items = listOf(
        HomeListItem.Header(HomeListItem.HeaderType.SUGGEST),
        HomeListItem.SuggestListState(
          comics = emptyList(),
          isLoading = false,
          errorMessage = null
        ),
        HomeListItem.Header(HomeListItem.HeaderType.TOP_MONTH),
        HomeListItem.TopMonthListState(
          comics = emptyList(),
          isLoading = false,
          errorMessage = null
        ),
        HomeListItem.Header(HomeListItem.HeaderType.UPDATED),
        HomeListItem.UpdatedItem.Loading
      ),
      refreshLoading = false,
      updatedPage = 1
    )
  }
}

sealed class HomeViewIntent : Intent {
  object Initial : HomeViewIntent()
  object Refresh : HomeViewIntent()
  object LoadNextPageUpdatedComic : HomeViewIntent()
  object RetrySuggest : HomeViewIntent()
  object RetryTopMonth : HomeViewIntent()
  object RetryUpdate : HomeViewIntent()
}

sealed class HomePartialChange {
  abstract fun reducer(state: HomeViewState): HomeViewState

  sealed class SuggestHomePartialChange : HomePartialChange() {
    override fun reducer(state: HomeViewState): HomeViewState {
      return when (this) {
        is HomePartialChange.SuggestHomePartialChange.Data -> {
          state.copy(
            items = state.items.map {
              if (it is HomeListItem.SuggestListState) {
                it.copy(
                  comics = this.comics,
                  isLoading = false,
                  errorMessage = null
                )
              } else {
                it
              }
            }
          )
        }
        HomePartialChange.SuggestHomePartialChange.Loading -> {
          state.copy(
            items = state.items.map {
              if (it is HomeListItem.SuggestListState) {
                it.copy(isLoading = true)
              } else {
                it
              }
            }
          )
        }
        is HomePartialChange.SuggestHomePartialChange.Error -> {
          state.copy(
            items = state.items.map {
              if (it is HomeListItem.SuggestListState) {
                it.copy(
                  isLoading = false,
                  errorMessage = getMessageFromError(this.error)
                )
              } else {
                it
              }
            }
          )
        }
      }
    }

    data class Data(val comics: List<Comic>) : SuggestHomePartialChange()
    object Loading : SuggestHomePartialChange()
    data class Error(val error: com.hoc.comicapp.data.models.ComicAppError) :
      SuggestHomePartialChange()
  }

  sealed class TopMonthHomePartialChange : HomePartialChange() {
    override fun reducer(state: HomeViewState): HomeViewState {
      return when (this) {
        is HomePartialChange.TopMonthHomePartialChange.Data -> {
          state.copy(
            items = state.items.map {
              if (it is HomeListItem.TopMonthListState) {
                it.copy(
                  comics = this.comics,
                  isLoading = false,
                  errorMessage = null
                )
              } else {
                it
              }
            }
          )
        }
        HomePartialChange.TopMonthHomePartialChange.Loading -> {
          state.copy(
            items = state.items.map {
              if (it is HomeListItem.TopMonthListState) {
                it.copy(isLoading = true)
              } else {
                it
              }
            }
          )
        }
        is HomePartialChange.TopMonthHomePartialChange.Error -> {
          state.copy(
            items = state.items.map {
              if (it is HomeListItem.TopMonthListState) {
                it.copy(
                  isLoading = false,
                  errorMessage = getMessageFromError(this.error)
                )
              } else {
                it
              }
            }
          )
        }
      }
    }

    data class Data(val comics: List<Comic>) : TopMonthHomePartialChange()
    object Loading : TopMonthHomePartialChange()
    data class Error(val error: com.hoc.comicapp.data.models.ComicAppError) :
      TopMonthHomePartialChange()
  }

  sealed class UpdatedPartialChange : HomePartialChange() {
    override fun reducer(state: HomeViewState): HomeViewState {
      return when (this) {
        is HomePartialChange.UpdatedPartialChange.Data -> {
          state.copy(
            items = state
              .items
              .filterNot { it is HomeListItem.UpdatedItem.Loading || it is HomeListItem.UpdatedItem.Error } +
              this.comics.map { HomeListItem.UpdatedItem.ComicItem(it) },
            updatedPage = state.updatedPage + 1
          )
        }
        HomePartialChange.UpdatedPartialChange.Loading -> {
          state.copy(
            items = state
              .items
              .filterNot { it is HomeListItem.UpdatedItem.Loading || it is HomeListItem.UpdatedItem.Error } +
              HomeListItem.UpdatedItem.Loading
          )
        }
        is HomePartialChange.UpdatedPartialChange.Error -> {
          state.copy(
            items = state
              .items
              .filterNot { it is HomeListItem.UpdatedItem.Loading || it is HomeListItem.UpdatedItem.Error } +
              HomeListItem.UpdatedItem.Error(getMessageFromError(this.error))
          )
        }
      }
    }

    data class Data(val comics: List<Comic>) : UpdatedPartialChange()
    object Loading : UpdatedPartialChange()
    data class Error(val error: com.hoc.comicapp.data.models.ComicAppError) : UpdatedPartialChange()
  }

  object RefreshSuccess : HomePartialChange() {
    override fun reducer(state: HomeViewState) = state.copy(refreshLoading = false)
  }

  data class RefreshFailure(val error: ComicAppError) : HomePartialChange() {
    override fun reducer(state: HomeViewState): HomeViewState = state.copy(refreshLoading = false)
  }
}

sealed class HomeSingleEvent : SingleEvent {
  data class MessageEvent(val message: String) : HomeSingleEvent()
}