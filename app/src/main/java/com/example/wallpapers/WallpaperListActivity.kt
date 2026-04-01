package com.example.wallpapers

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WallpaperListActivity : AppCompatActivity() {

    lateinit var recyclerView: RecyclerView
    lateinit var swipeRefresh: SwipeRefreshLayout
    lateinit var searchbar: EditText
    private lateinit var wallpaperAdapter: WallpaperAdapter

    private var currentPage = 1
    private var currentQuery: String? = null
    private var isLoading = false

    private val repo = WallpaperRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_wallpaper_list)
        bindViews()
        setupRecyclerView()
        setupSwipeRefresh()
        setupSearchbar()
        fetchWallpapers(currentPage)
    }

    private fun bindViews() {
        recyclerView = findViewById(R.id.rvWallpapers)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        searchbar = findViewById(R.id.etSearch)
    }

    private fun setupRecyclerView() {
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        wallpaperAdapter = WallpaperAdapter { currentWallpaper ->
            val intent = Intent(this, WallpaperDetailActivity::class.java).apply {
                putExtra("IMAGE_URL", currentWallpaper.largeImageURL)
            }
            startActivity(intent)
        }

        recyclerView.adapter = wallpaperAdapter
        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        recyclerView.layoutManager = layoutManager

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as StaggeredGridLayoutManager

                val lastVisibleItemPositions = layoutManager.findLastVisibleItemPositions(null)

                val lastVisibleItem = lastVisibleItemPositions.maxOrNull() ?: 0
                val totalItemCount = layoutManager.itemCount

                if (!isLoading && totalItemCount <= lastVisibleItem + 1) {
                    isLoading = true
                    currentPage++
                    fetchWallpapers(currentPage, currentQuery)
                }
            }
        })
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            currentPage = 1
            fetchWallpapers(currentPage, currentQuery)
        }
    }

    private fun setupSearchbar() {
        searchbar.setOnEditorActionListener { v, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                currentPage = 1
                currentQuery = searchbar.text.toString().ifEmpty { null }

                fetchWallpapers(currentPage, currentQuery)

                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                searchbar.clearFocus()

                return@setOnEditorActionListener true
            } else {
                return@setOnEditorActionListener false
            }
        }
    }

    private fun fetchWallpapers(page: Int, query: String? = null) {
        if (page > 1) {
            recyclerView.post {
                wallpaperAdapter.addLoadingView()
            }
        }

        lifecycleScope.launch {
            val images = repo.getWallpapers(page, query)

            hideLoadingIndicators(page)

            if (currentPage == 1)
                wallpaperAdapter.setList(images)
            else
                wallpaperAdapter.addToList(images)
        }

//        val retrofit = Retrofit.Builder()
//            .baseUrl("https://pixabay.com/")
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//
//        val service = retrofit.create(WallpaperApiService::class.java)
//
//        service.getWallpapers(apiKey, page, query).enqueue(object : retrofit2.Callback<PixabayResponse> {
//
//            override fun onResponse(call: Call<PixabayResponse>, response: retrofit2.Response<PixabayResponse>) {
//                hideLoadingIndicators(page)
//
//                if (response.isSuccessful) {
//                    val images = response.body()?.hits ?: emptyList()
//                    if (currentPage == 1)
//                        wallpaperAdapter.setList(images)
//                    else
//                        wallpaperAdapter.addToList(images)
//                }
//            }
//
//            override fun onFailure(call: Call<PixabayResponse>, t: Throwable) {
//                hideLoadingIndicators(page)
//            }
//
//        })
    }

    private fun hideLoadingIndicators(page: Int) {
        isLoading = false
        swipeRefresh.isRefreshing = false

        if (page > 1) {
            wallpaperAdapter.removeLoadingView()
        }
    }

}