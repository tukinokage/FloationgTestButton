package com.shay.floatingtestbutton

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import android.util.ArrayMap
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.ceil


// TODO:2023/2/22
//  待优化:
//  1.增加对fragment的支持
//  2.前台服务开启关闭按钮
//  3.dialog创建的新window会覆盖悬浮按钮
/**
 * CREATE BY Shay
 * DATE BY 2023/2/22 17:05 星期三
 * DESCRIBE 不可直接用於佈局中，請使用[GlobalFloatButton.init(context)]初始化
 * 在activity中使用[TestClick]注釋進行點擊回調
 *
 * 当创建启动时的第一个activtiy被销毁时，则会选择默认栈底的actitviy重新创建
 * 没有使用wm添加，wm添加的控件和decorview处于同一层级，但是需要悬浮窗权限才能在多个界面显示
 * 当前是处于contentview同一层级，新界面不断添加，因此不需要权限
 * <p>
 * contact me by woshihaorenla@gmail.com or 3355847539@qq.com
 */

class GlobalFloatButton :androidx.appcompat.widget.AppCompatImageButton{
   private constructor(context: Context) : super(context)
    /**
     * 相对按钮的坐标
     * */
    var mTouchStartX = 0f
    var mTouchStartY = 0f

    var mStartX = 0f
    var mStartY = 0f
    var mLastX = 0f
    var mLastY = 0f

    var mDownTime = 0L
    var mUpTime = 0L
    var isMove = false

    var defaultWidth = 110
    var defaultHeight = 110

    //触摸时的坐标
    var touchX = 0
    var touchY = 0

    val layoutParams = FrameLayout.LayoutParams(defaultWidth, defaultHeight)
    val mutex = Mutex()
    var listener:SpeakListener = object :SpeakListener{
        override fun onClick(view: View) {
            GlobalScope.launch {
                //防止多次点击
                mutex.withLock {
                    val activityList = withContext(Dispatchers.IO) {
                        getActivitiesByApplication(application)
                    }
                    if (!activityList.isNullOrEmpty()){
                        withContext(Dispatchers.IO) {
                            //取出当前显示的栈顶activity, activity为空即退出
                            val topActivity = activityList.first() ?: return@withContext
                            getAnnotationClickMethod(topActivity){
                                    it.forEach { method ->
                                        withContext(Dispatchers.Main) {
                                            method.invoke(topActivity)
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    companion object{
         var windowManager:WindowManager? = null;
        /**当前显示界面的布局*/
         var contentLayout:WeakReference<FrameLayout>? = null;
        private val allActivtiys:ArrayMap<Activity, Activity>by lazy {
            ArrayMap()
        }
        private lateinit var application: Application
        /**用于创建按钮的activtiy*/
        private var createBtnActivity:WeakReference<Activity>? = null
        @SuppressLint("StaticFieldLeak")
        var instance: GlobalFloatButton? = null
            private set

        private fun destroyGButton(){
            instance?.let {
                //windowManager?.removeView(it)
                try {
                    contentLayout?.get()?.removeView(it)
                }catch (e:java.lang.IllegalStateException){
                }finally { }
            }
            windowManager = null;
            instance = null
            /**用于创建btn时的activity*/
            createBtnActivity?.clear()
        }

        /**
         * 自动选择栈底ctivtiy
         * */
        private fun createGButtonAuto(){
            if(allActivtiys.isNotEmpty()){
                val first = allActivtiys.entries.first()
                first?.value?.let {
                    createGButton(it) }
            }

        }

        private fun layoutBtn(layout: FrameLayout){
            contentLayout?.get()?.removeView(instance)/*先从之前的layout移除*/
            contentLayout = WeakReference(layout)
            contentLayout?.get()?.addView(instance)

        }

        private fun createGButton(activity:Activity){
            createBtnActivity = WeakReference(activity)
            // windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager;
            val layout =
                activity.window.decorView.findViewById<FrameLayout>(android.R.id.content)
            instance = GlobalFloatButton(activity).apply { resgister() }
            layout.addView(instance)
        }

        /**
         * 需要在application启动
         * */
        @JvmStatic
        fun init(context: Application){
            application = context
            application.registerActivityLifecycleCallbacks(object :ActivityLifecycleCallbacks{

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    if (allActivtiys.containsKey(activity)){
                        return
                    }
                    allActivtiys[activity] = activity

                }
                /**
                 * 默认第一次启动时activity用于创建按钮
                 * */
                override fun onActivityStarted(activity: Activity) {

                    val layout =
                        activity.window.decorView.findViewById<FrameLayout>(android.R.id.content)
                    if (instance == null || createBtnActivity == null || createBtnActivity!!.get() == null){//创建btn
                        contentLayout = WeakReference(layout)
                        createGButton(activity)
                    }else{//新界面，直接添加已有btn
                        layoutBtn(layout)
                    }
                }
                override fun onActivityResumed(activity: Activity) {
                   // layoutBtn()

                }
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                /**
                 * 如果首次创建按钮的activity被销毁了，则需要销毁原来的按钮，
                 * 并重新创建，重新创建的选择activtiy栈的首个activtiy
                 * 如果已经是最后一个activtiy，则走正常onCreate流程创建
                 * 如果新栈底是当前activity，layoutBtn()只做刷新操作
                 * */
                //TODO:此处点返回时生命周期有问题需要处理
                override fun onActivityDestroyed(activity: Activity) {
                    allActivtiys.remove(activity)
                    if (activity == createBtnActivity?.get() ?: null){
                        createBtnActivity?.clear()
                        destroyGButton()
                        createGButtonAuto()
                        /**重新创建后，由于用于的创建的activity大概率不是当前显示的界面，当前显示的界面也要重新渲添加*/
                        contentLayout?.get()?.let { layoutBtn(it) }
                    }
                }

            })
        }
    }

    /**初始化默認的參數*/
    private fun resgister() {
        val dm = resources.displayMetrics
        /*Requires android.Manifest.permission.SYSTEM_ALERT_WINDOW permission.*/
        //layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        val widthPixels: Int = dm.widthPixels
        val heightPixels: Int = dm.heightPixels
      /*  layoutParams.apply {
            //默认
         gravity = Gravity.BOTTOM
        }*/
        x = (widthPixels-defaultWidth).toFloat();  //设置位置像素
        y = heightPixels/2f + defaultHeight
        setLayoutParams(layoutParams)
        background = context.getDrawable(R.drawable.bug_report)
        elevation = 100f
        //windowManager?.addView(this, layoutParams);
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 获得状态栏高度
        val statusHeight = try {
            val resourceId =
                context.resources.getIdentifier("status_bar_height", "dimen", "android")
            ceil(context.resources.getDimension(resourceId).toDouble()).toInt()
        } catch (e: Exception) {
            75
        }
        //工具栏高度
        val tv = TypedValue()
        var actionBarHeight = try {
            if (allActivtiys.entries.last().value.actionBar == null) {
                0
            } else if (context.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                TypedValue.complexToDimensionPixelSize(tv.data, context.resources.displayMetrics)
            } else 0
        } catch (e: Exception) {
            0
        }
        //获取相对屏幕的坐标，即以屏幕左上角为原点
        touchX = event.rawX.toInt()
        touchY = (event.rawY - statusHeight - actionBarHeight).toInt() //statusHeight是系统状态栏的高度，actionBarHeight主题带的工具栏高度
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
               // setImageResource(R.drawable.btn_voice_pressed)
                mTouchStartX = event.x
                mTouchStartY = event.y
                mStartX = event.rawX
                mStartY = event.rawY
                mDownTime = System.currentTimeMillis()
                isMove = false
            }
            MotionEvent.ACTION_MOVE -> {
                updateViewPosition(touchX, touchY)
                isMove = true
            }
            MotionEvent.ACTION_UP -> {
                //setImageResource(R.drawable.btn_voice_rest)
                mLastX = event.rawX
                mLastY = event.rawY
                mUpTime = System.currentTimeMillis()
                //按下到抬起的时间大于500毫秒,并且抬手到抬手绝对值大于20像素处理点击事件
                if (mUpTime - mDownTime < 500) {
                    if (abs(mStartX - mLastX) < 20.0 && abs(mStartY - mLastY) < 20.0) {
                        listener?.onClick(this)
                    }
                }
            }
        }
        return true
    }

    private fun updateViewPosition(x:Int, y:Int) {
        this.x =  x - mTouchStartX;
        this.y =  y- mTouchStartY;

    //windowManager?.updateViewLayout(this, layoutParams);
    }


    interface SpeakListener {
        fun onClick(view: View)
    }

}