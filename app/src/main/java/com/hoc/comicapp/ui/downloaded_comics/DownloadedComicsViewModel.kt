package com.hoc.comicapp.ui.downloaded_comics

import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.*
import com.hoc.comicapp.utils.notOfType
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy

class DownloadedComicsViewModel(
  private val rxSchedulerProvider: RxSchedulerProvider,
  private val interactor: Interactor
) : BaseViewModel<ViewIntent, ViewState, SingleEvent>() {
  override val initialState = ViewState.initial()

  private val intentS = PublishRelay.create<ViewIntent>()

  override fun processIntents(intents: Observable<ViewIntent>) = intents.subscribe(intentS)!!

  private val intentToChanges = ObservableTransformer<ViewIntent, PartialChange> { intents ->
    intents
      .ofType<ViewIntent.Initial>()
      .flatMap {
        interactor
          .getDownloadedComics()
          .observeOn(rxSchedulerProvider.main)
          .doOnNext {
            val errorChange = it as? PartialChange.Error ?: return@doOnNext
            sendEvent(SingleEvent.Message("Error occurred: ${errorChange.error.getMessage()}"))
          }
      }
  }

  init {
    intentS
      .compose(intentFilter)
      .compose(intentToChanges)
      .scan(initialState, reducer)
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy(onNext = ::setNewState)
      .addTo(compositeDisposable)
  }

  private companion object {
    val intentFilter = ObservableTransformer<ViewIntent, ViewIntent> { intents ->
      intents.publish { shared ->
        Observable.mergeArray(
          shared.ofType<ViewIntent.Initial>().take(1),
          shared.notOfType<ViewIntent.Initial, ViewIntent>()
        )
      }
    }

    val reducer = BiFunction<ViewState, PartialChange, ViewState> { vs, change ->
      when (change) {
        is PartialChange.Data -> {
          vs.copy(
            isLoading = false,
            error = null,
            comics = change.comics
          )
        }
        is PartialChange.Error -> {
          vs.copy(
            isLoading = false,
            error = change.error
          )
        }
        PartialChange.Loading -> {
          vs.copy(isLoading = true)
        }
      }
    }
  }
}