package com.hoc.comicapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hoc.comicapp.R
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.snack
import com.jakewharton.rxbinding3.recyclerview.scrollStateChanges
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

@ExperimentalCoroutinesApi
class HomeFragment : Fragment() {
  private val homeViewModel by viewModel<HomeViewModel>()
  private val homeAdapter = HomeAdapter(this)
  private val compositeDisposableDisposeOnPause = CompositeDisposable()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.fragment_home, container, false).also {
    Timber.d("HomeFragment::onCreateView")
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    Timber.d("HomeFragment::onViewCreated")

    initView()
    observeViewModel()
  }

  private fun observeViewModel() {
    homeViewModel.state.observe(this) { (items, refreshLoading) ->
      Timber.d("[VER] ${items.size}")

      homeAdapter.submitList(items)
      if (!refreshLoading) {
        swipe_refresh_layout.isRefreshing = false
      }
    }
    homeViewModel.singleEvent.observeEvent(this) {
      when (it) {
        is HomeSingleEvent.MessageEvent -> {
          view?.snack(it.message)
        }
      }
    }
  }

  private fun initView() {
    swipe_refresh_layout.setColorSchemeColors(*resources.getIntArray(R.array.swipe_refresh_colors))

    recycler_home.run {
      setHasFixedSize(true)
      layoutManager = GridLayoutManager(context, 2).apply {
        spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
          override fun getSpanSize(position: Int): Int {
            return if (homeAdapter.getItemViewType(position) == HomeAdapter.COMIC_ITEM_VIEW_TYPE) {
              1
            } else {
              2
            }
          }
        }
      }
      adapter = homeAdapter
    }
  }

  override fun onResume() {
    super.onResume()
    Timber.d("HomeFragment::onResume")

    homeViewModel.processIntents(
      Observable.mergeArray(
        Observable.just(HomeViewIntent.Initial),
        swipe_refresh_layout.refreshes().map { HomeViewIntent.Refresh },
        loadNextPageIntent(),
        homeAdapter.suggestRetryObservable.map { HomeViewIntent.RetrySuggest },
        homeAdapter.topMonthRetryObservable.map { HomeViewIntent.RetryTopMonth },
        homeAdapter.updatedRetryObservable.map { HomeViewIntent.RetryUpdate }
      )
    ).addTo(compositeDisposableDisposeOnPause)
  }

  private fun loadNextPageIntent(): Observable<HomeViewIntent.LoadNextPageUpdatedComic> {
    return recycler_home.scrollStateChanges()
      .filter { it == RecyclerView.SCROLL_STATE_IDLE }
      .filter {
        (recycler_home.layoutManager as GridLayoutManager).findLastCompletelyVisibleItemPosition() +
          VISIBLE_THRESHOLD >= homeAdapter.itemCount
      }
      .map { HomeViewIntent.LoadNextPageUpdatedComic }
  }

  override fun onPause() {
    super.onPause()
    Timber.d("HomeFragment::onPause")

    compositeDisposableDisposeOnPause.clear()
  }

  private companion object {
    const val VISIBLE_THRESHOLD = 5
  }
}
