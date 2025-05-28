package io.legado.app.ui.widget.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogPhotoViewBinding
import io.legado.app.help.book.BookHelp
import io.legado.app.help.glide.ImageLoader
import io.legado.app.help.glide.OkHttpModelLoader
import io.legado.app.model.BookCover
import io.legado.app.model.ImageProvider
import io.legado.app.model.ReadBook
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding
import androidx.core.graphics.drawable.toDrawable

/**
 * 显示图片
 */
class PhotoDialog() : BaseDialogFragment(R.layout.dialog_photo_view), View.OnClickListener {

    constructor(src: String, sourceOrigin: String? = null) : this() {
        arguments = Bundle().apply {
            putString("src", src)
            putString("sourceOrigin", sourceOrigin)
        }
    }

    private val binding by viewBinding(DialogPhotoViewBinding::bind)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        // 注意：这里不调用 WindowCompat.setDecorFitsSystemWindows
        dialog.window?.let { window ->
            window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            // 1. 允许内容绘制到系统栏区域后面 (对于全屏和刘海屏很重要)
            // !! 将 setDecorFitsSystemWindows 移动到这里 !!
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // 2. 设置 Dialog 大小为全屏
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setGravity(Gravity.CENTER) // 确保居中

            // 3. 设置全屏和沉浸式体验
            setFullScreenAndImmersive(window)

            // 可选：如果边距的问题，可以清除 DecorView 的 padding
            // val decorView = window.decorView
            // decorView.setPadding(0, 0, 0, 0)
        }
    }

    private fun setFullScreenAndImmersive(window: Window) {
        // 使用 WindowInsetsControllerCompat 来控制系统栏 (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller =
                WindowInsetsControllerCompat(window, window.decorView)
            // 隐藏状态栏和导航栏
            controller.hide(WindowInsetsCompat.Type.systemBars())
            // 设置沉浸式行为，当用户滑动时系统栏短暂显示然后再次隐藏
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            // 对于旧版本 (API 16-29)
            @Suppress("DEPRECATION")
            var uiFlags = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // 关键：使用 STICKY 版本，用户滑动后系统栏自动隐藏
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // 隐藏导航栏
                    or View.SYSTEM_UI_FLAG_FULLSCREEN) // 隐藏状态栏

            window.decorView.systemUiVisibility = uiFlags

            // 为了在 API 19 (KitKat) 以下的设备上更好地隐藏状态栏，
            // FLAG_FULLSCREEN 是必须的。
            // 对于 API 16-18，导航栏可能无法完全以这种方式隐藏或表现可能不同。
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        }

        // 处理刘海屏 (API 28+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val attributes = window.attributes
            attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = attributes
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundResource(R.color.transparent)
    }

    @SuppressLint("CheckResult")
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val arguments = arguments ?: return
        val src = arguments.getString("src") ?: return
        ImageProvider.get(src)?.let {
            binding.photoView.setImageBitmap(it)
            return
        }
        val file = ReadBook.book?.let { book ->
            BookHelp.getImage(book, src)
        }
        if (file?.exists() == true) {
            ImageLoader.load(requireContext(), file)
                .error(R.drawable.image_loading_error)
                .dontTransform()
                .downsample(DownsampleStrategy.NONE)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .fitCenter()
                .into(binding.photoView)
        } else {
            ImageLoader.load(requireContext(), src).apply {
                arguments.getString("sourceOrigin")?.let { sourceOrigin ->
                    apply(RequestOptions().set(OkHttpModelLoader.sourceOriginOption, sourceOrigin))
                }
            }.error(BookCover.defaultDrawable)
                .dontTransform()
                .downsample(DownsampleStrategy.NONE)
                .into(binding.photoView)
        }

        binding.photoView.setOnClickListener(this)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val activity: Activity? = activity
        if (activity is ReadBookActivity) {
            // activity.setReadAloud(true)
            // TODO("Not yet implemented")
            return
        }
    }

    override fun onClick(p0: View?) {
        dismissAllowingStateLoss()
    }

}
