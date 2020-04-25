package com.hoc.comicapp.ui.chapter_detail

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.viewpager2.widget.ViewPager2
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.activity.main.MainActivity
import com.hoc.comicapp.base.BaseFragment
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.SingleEvent
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.ViewIntent
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.ViewState
import com.hoc.comicapp.utils.snack
import com.hoc.comicapp.utils.unit
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.checkedChanges
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_chapter_detail.*
import org.koin.androidx.scope.lifecycleScope
import org.koin.androidx.viewmodel.scope.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import kotlin.LazyThreadSafetyMode.NONE

class ChapterDetailFragment : BaseFragment<
    ViewIntent,
    ViewState,
    SingleEvent,
    ChapterDetailViewModel
    >(R.layout.fragment_chapter_detail) {
  private val navArgs by navArgs<ChapterDetailFragmentArgs>()
  override val viewModel by lifecycleScope.viewModel<ChapterDetailViewModel>(owner = this) {
    parametersOf(navArgs.isDownloaded)
  }

  private val mainActivity get() = requireActivity() as MainActivity
  private var shouldEmitSelectedItem = false

  private val chapterImageAdapter by lazy(NONE) { ChapterImageAdapter(GlideApp.with(this)) }
  private val allChaptersAdapter by lazy(NONE) {
    ArrayAdapter(
      requireContext(),
      android.R.layout.simple_spinner_dropdown_item,
      mutableListOf(
        ViewState.Chapter(
          link = navArgs.chapter.chapterLink,
          name = navArgs.chapter.chapterName
        )
      )
    )
  }

  override fun onDestroyView() {
    super.onDestroyView()
    recycler_images.adapter = null
    spinner_chapters.adapter = null
  }

  //region Override BaseFragment
  override fun setupView(view: View, savedInstanceState: Bundle?) {
    recycler_images.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      adapter = chapterImageAdapter
    }

    spinner_chapters.adapter = allChaptersAdapter

    viewModel.state.safeValue?.detail?.let { detail ->
      allChaptersAdapter.clear()
      allChaptersAdapter.addAll(detail.chapters)

      val index = detail.chapters.indexOfFirst { it == detail.chapter }
      spinner_chapters.setSelection(index, false)

      Timber.tag("LoadChapter###").d("::initView $index $detail")
    }
  }

  override fun render(viewState: ViewState) {
    val (isLoading, isRefreshing, errorMessage, detail, @ViewPager2.Orientation orientation) = viewState
    Timber.d("chapter_detail_state=[$isLoading, $isRefreshing, $errorMessage, $detail, $orientation]")

    shouldEmitSelectedItem = false

    (recycler_images.layoutManager as LinearLayoutManager).orientation = orientation

    progress_bar.isVisible = isLoading

    group_error.isVisible = errorMessage !== null
    text_error_message.text = errorMessage

    detail ?: return
    mainActivity.setToolbarTitle(detail.chapter.name)

    allChaptersAdapter.clear()
    allChaptersAdapter.addAll(detail.chapters)

    val index = detail.chapters.indexOfFirst { it == detail.chapter }
    spinner_chapters.setSelection(index, false)
    Timber.tag("LoadChapter###").d("Index=$index, chapter=${detail.chapter.debug}")

    when (detail) {
      is ViewState.Detail.Data -> {
        chapterImageAdapter.submitList(detail.images)

        TransitionManager.beginDelayedTransition(bottom_nav, AutoTransition())
        button_prev.isInvisible = detail.prevChapterLink === null
        button_next.isInvisible = detail.nextChapterLink === null
      }
      is ViewState.Detail.Initial -> {
        chapterImageAdapter.submitList(emptyList())

        TransitionManager.beginDelayedTransition(bottom_nav, AutoTransition())
        button_prev.isInvisible = true
        button_next.isInvisible = true
      }
    }

    shouldEmitSelectedItem = true
  }

  override fun handleEvent(event: SingleEvent) {
    return when (event) {
      is SingleEvent.MessageEvent -> {
        view?.snack(event.message)
      }
    }.unit
  }

  override fun viewIntents(): Observable<ViewIntent> {
    val chapterItemSelections = Observable.create<ViewState.Chapter> { emitter ->
      spinner_chapters.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {}

        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
          if (shouldEmitSelectedItem && !emitter.isDisposed) {
            emitter.onNext(parent.getItemAtPosition(position) as ViewState.Chapter)
          }
        }
      }

      emitter.setCancellable { spinner_chapters.onItemSelectedListener = null }
    }

    return Observable.mergeArray(
      Observable.just(
        ViewIntent.Initial(
          ViewState.Chapter(
            name = navArgs.chapter.chapterName,
            link = navArgs.chapter.chapterLink
          )
        )
      ),
      button_next
        .clicks()
        .map { ViewIntent.LoadNextChapter },
      button_prev
        .clicks()
        .map { ViewIntent.LoadPrevChapter },
      button_retry
        .clicks()
        .map { ViewIntent.Retry },
      chapterItemSelections
        .map { ViewIntent.LoadChapter(it) },
      switch_orientation
        .checkedChanges()
        .map { ViewIntent.ChangeOrientation(if (it) RecyclerView.VERTICAL else RecyclerView.HORIZONTAL) }
    ).doOnNext { Timber.tag("LoadChapter###").d("Intent $it") }
  }
  //endregion
}