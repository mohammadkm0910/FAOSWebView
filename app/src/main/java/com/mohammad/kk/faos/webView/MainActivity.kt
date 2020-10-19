package com.mohammad.kk.faos.webView

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.graphics.Bitmap
import android.net.*
import android.net.http.SslError
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.webkit.*

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.mohammad.kk.faos.webView.tab.Tab
import com.mohammad.kk.faos.webView.tab.TabRecyclerAdapter
import com.mohammad.kk.faos.webView.views.RootSnackBar
import com.mohammad.kk.faos.webView.views.ToastyColor
import com.mohammad.kk.faos.webView.views.WebViewScroller
import kotlinx.android.synthetic.main.action_main_bar.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.tools_dialog_page.view.*
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity() {
    @Suppress("DEPRECATION")
    private fun createWebView():WebViewScroller {
        val webView = WebViewScroller(this)
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.webViewClient = object : WebViewClient(){
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                view!!.loadUrl(url!!)
                if ("mailto:" in url || "sms:" in url || "tel:" in url){
                    view.stopLoading()
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                return true
            }
            override fun onLoadResource(view: WebView?, url: String?) {
                super.onLoadResource(view, url)
                view!!.evaluateJavascript(jsDesktopMode, null)
            }
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressWebView.visibility = View.VISIBLE
                progressWebView.progress = 0
                btnRefresh.setImageResource(R.drawable.ic_close)
                if (!isInternet) RootSnackBar(this@MainActivity,"لطفا اتصال اینترنت را بررسی کنید").show()
                isFinishedPage = false
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressWebView.visibility = View.GONE
                progressWebView.progress = 100
                btnRefresh.setImageResource(R.drawable.ic_refresh)
                isFinishedPage = true
                tabRecyclerAdapter.notifyDataSetChanged()
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                super.onReceivedSslError(view, handler, error)
                val builder = AlertDialog.Builder(this@MainActivity)
                var message = "خطای گواهی SSL."
                when(error?.primaryError){
                    SslError.SSL_UNTRUSTED -> message = "مرجع گواهی مورد اعتماد نیست."
                    SslError.SSL_EXPIRED -> message = "گواهی منقضی شده است."
                    SslError.SSL_IDMISMATCH -> message = "عدم تطابق نام میزبان با گواهی."
                    SslError.SSL_NOTYETVALID -> message = "گواهی هنوز معتبر نیست."
                }
                message += "\n" + "آیا به هر حال می خواهید ادامه دهید؟"
                builder.setTitle("خطای گواهی SSL.")
                builder.setMessage(message)
                builder.setPositiveButton("ادامه") { dialog, _ ->
                    handler?.proceed()
                    dialog.dismiss()
                }
                builder.setNegativeButton("منصرف"){ dialog, _ ->
                    handler?.cancel()
                    dialog.dismiss()
                }
                val dialog = builder.create()
                dialog.show()
            }

        }
        webView.webChromeClient = object : WebChromeClient(){
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressWebView.progress = newProgress
            }
        }
        val onComplete: BroadcastReceiver = object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                ToastyColor(applicationContext, "دانلود با موفقیت انجام شد", 2).show()
            }
        }
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimeType)
            val cookies = CookieManager.getInstance().getCookie(url)
            request.addRequestHeader("cookie", cookies)
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("در حال دانلود فایل...")
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(
                    url,
                    contentDisposition,
                    mimeType
                )
            )
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            ToastyColor(applicationContext, "شروع دانلود", 1).show()
            registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
        webView.loadUrl(googleUrl)
        return webView
    }
    private val tabs:ArrayList<Tab> = ArrayList()
    private var currentTabIndex by Delegates.notNull<Int>()
    private lateinit var tabRecyclerAdapter: TabRecyclerAdapter
    private fun newTab(url: String) {
        val webView = createWebView()
        webView.visibility = View.GONE
        val tab = Tab(webView)
        tabs.add(tab)
        webView.loadUrl(url)
        webViews.addView(webView)
        tabCounter()
    }
    private fun getCurrentTab():Tab = tabs[currentTabIndex]
    private fun getCurrentWebView():WebViewScroller = getCurrentTab().webView
    private fun switchToTab(tab: Int) {
        getCurrentWebView().visibility = View.GONE
        currentTabIndex = tab
        getCurrentWebView().visibility = View.VISIBLE
        getCurrentWebView().requestFocus()
    }
    override fun onStart() {
        super.onStart()
        cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        val networkCallback = object : ConnectivityManager.NetworkCallback(){
            override fun onLost(network: Network) {
                super.onLost(network)
                isInternet = false
            }
            override fun onUnavailable() {
                super.onUnavailable()
                isInternet = false

            }
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                isInternet = true
            }
        }
        cm.registerNetworkCallback(networkRequest,networkCallback)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        currentTabIndex = 0
        newTab(googleUrl)
        getCurrentWebView().visibility = View.VISIBLE
        getCurrentWebView().requestFocus()

        tabRecyclerAdapter = TabRecyclerAdapter(this, tabs){
            switchToTab(it)
            tabRecyclerAdapter.sendIndex = it
            tabRecyclerAdapter.notifyDataSetChanged()
        }
        recyclerTabList.layoutManager = LinearLayoutManager(this)
        recyclerTabList.adapter = tabRecyclerAdapter
        setupBottomNavigation()
        actionTopUrl()
        tabNumber.setOnClickListener {
            mainDrawer.openDrawer(endDrawer)
        }
    }
    private fun setupBottomNavigation() {
        bottomNavTab.setOnNavigationItemSelectedListener {
            when(it.itemId){
                R.id.addTab -> {
                    newTab(googleUrl)
                    switchToTab(tabs.size - 1)
                    tabRecyclerAdapter.sendIndex = tabs.size - 1
                    tabRecyclerAdapter.notifyDataSetChanged()
                }
                R.id.forward -> {
                    if (getCurrentWebView().canGoForward())
                        getCurrentWebView().goForward()
                    else
                        ToastyColor(this, "چیزی برای رفتن به جلو وجود ندارد.", 3).show()
                }
                R.id.removeTab -> closeCurrentTab()
                R.id.back -> {
                    if (getCurrentWebView().canGoBack())
                        getCurrentWebView().goBack()
                    else
                        ToastyColor(this, "چیزی برای رفتن به عقب وجود ندارد.", 3).show()
                }
            }
            true
        }
        bottomNavBookmark.setOnNavigationItemSelectedListener {
            when(it.itemId){
                R.id.toggleWebPage -> {
                    val alertDialog = AlertDialog.Builder(this)
                    val view = LayoutInflater.from(this).inflate(R.layout.tools_dialog_page,findViewById(R.id.toolsDialogPage))
                    val toggleDesktopMode = view.toggleDesktopMode
                    val leftDesktop = ContextCompat.getDrawable(this,R.drawable.ic_desktop)
                    val leftMobile = ContextCompat.getDrawable(this,R.drawable.ic_mobile)
                    leftDesktop!!.setBounds(0,0,60,60)
                    leftMobile!!.setBounds(0,0,60,60)
                    toggleDesktopMode.setCompoundDrawables(if (getCurrentTab().isDesktopMode) leftDesktop else leftMobile,null,null,null)
                    toggleDesktopMode.setOnClickListener {
                        getCurrentTab().isDesktopMode = !getCurrentTab().isDesktopMode
                        getCurrentWebView().setDesktopMode(getCurrentTab().isDesktopMode)
                        toggleDesktopMode.setCompoundDrawables(if (getCurrentTab().isDesktopMode) leftDesktop else leftMobile,null,null,null)
                    }
                    alertDialog.setView(view)
                    alertDialog.setTitle("ابزار های صفحه")
                    mainDrawer.closeDrawer(startDrawer)
                    alertDialog.show()
                }
                R.id.readerModeWebPage -> {}
            }
            true
        }
    }
    companion object {
        private lateinit var cm:ConnectivityManager
        private lateinit var networkRequest:NetworkRequest
        private var isInternet = false
        private var isFinishedPage = false
    }
    private fun actionTopUrl() {
        btnRefresh.setOnClickListener {
            if (isFinishedPage){
                getCurrentWebView().reload()
            } else {
                getCurrentWebView().stopLoading()
            }
        }
    }
    override fun onBackPressed() {
        when {
            getCurrentWebView().canGoBack() -> {
                getCurrentWebView().goBack()
            }
            tabs.size > 1 -> {
                closeCurrentTab()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }
    private fun closeCurrentTab() {
        webViews.removeView(getCurrentWebView())
        getCurrentWebView().destroy()
        tabs.removeAt(currentTabIndex)
        if (currentTabIndex >= tabs.size) currentTabIndex = tabs.size -1
        if (currentTabIndex == -1) {
            newTab(googleUrl)
            currentTabIndex = 0
        }
        getCurrentWebView().visibility = View.VISIBLE
        getCurrentWebView().requestFocus()
        tabRecyclerAdapter.sendIndex = currentTabIndex
        tabRecyclerAdapter.notifyDataSetChanged()
        tabCounter()
    }
    private fun tabCounter() {
        tabNumber.text = "${tabs.size}"
    }
}