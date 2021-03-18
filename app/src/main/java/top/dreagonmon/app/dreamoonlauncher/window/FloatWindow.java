package top.dreagonmon.app.dreamoonlauncher.window;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;


/*
 * Manage the float windows easily
 * Make the window looks like the window on Windows  :p
 * Created by Dreagonmon on 2017/1/15.
 * 记得先申请悬浮窗权限！如果只想用APPLICATION层面的窗口，请修改DEFAULT_WINDOW_TYPE
 */

public class FloatWindow
{
    public final int DEFAULT_WINDOW_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION;
    public final int scrWidth;
    public final int scrHeight;
    public int minWidth;
    public int maxWidth;
    public int minHeight;
    public int maxHeight;
    private final WindowManager wm;
    private final Context context;
    private boolean isShowing = false;
    private View view;//内容对象
    private View window;//窗口对象
    private WindowManager.LayoutParams lp;
    private TouchListenerMove listenerMove = new TouchListenerMove();
    private TouchListenerResize listenerResize = new TouchListenerResize();
    private Runnable onPressAction;//点击事件
    public FloatWindow(Context context)
    {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            DEFAULT_WINDOW_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
//        }else{
//            DEFAULT_WINDOW_TYPE = WindowManager.LayoutParams.TYPE_PHONE;
//        }
        this.wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.context = context;
        DisplayMetrics DM = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(DM);
        maxWidth = scrWidth = DM.widthPixels;
        maxHeight = scrHeight = DM.heightPixels;
        minWidth = 0;
        minHeight = 0;
        readyLp();
    }

    public FloatWindow(Context context,View view)
    {
        this(context);
        this.setView(view);
    }
    private void readyLp()
    {
        lp = new WindowManager.LayoutParams();
        lp.type = DEFAULT_WINDOW_TYPE;//悬浮窗口类型，记得给予权限
        //背景可点击，保持在屏幕内
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                |WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                |WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        lp.format = PixelFormat.RGBA_8888;//透明背景
        lp.gravity = Gravity.LEFT|Gravity.TOP;//重力左上角，方便调整大小
        lp.width = scrWidth;
        lp.height = scrHeight;
        lp.x = 0;
        lp.y = 0;
    }


    /*内部元素操作类方法*/
    /*
    * setWindow()与setView()的区别:
    * 一个会自动包裹进窗口内部，一个就是单纯的传入视图
    * 当使用了setView的时候，getWindow()与getView()效果一样，
    * 当使用了setWindow的时候，getWindow()返回窗口整体，getView()返回之前包裹的视图，*/
    public View setWindow(int styleLayoutID,int containerID, View v)
    {
        /*主要内容容器
        * 自定义layout布局，为styleLayoutID所指定的布局
        * 最后自由添加内容的FrameLayout容器的ID为window_container*/
        if (window == null||window.getId()!=styleLayoutID)
        {
            window = LayoutInflater.from(context).inflate(styleLayoutID,null);
        }
        FrameLayout container = (FrameLayout) window.findViewById(containerID);
        v.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));
        container.removeAllViews();
        container.addView(v);
        this.view = v;
        return window;
    }
    public View getWindow()
    {
        return window;
    }
    public void setView(View view)
    {
        this.view = window = view;
    }
    public View getView()
    {
        return view;
    }
    public View getElementById(int ID)
    {
        return window.findViewById(ID);
    }
    public WindowManager.LayoutParams getLayoutParams()
    {
        return lp;
    }
    public void updateWindow()//配合取出的LayoutParams使用，记得改过lp之后更新窗口！
    {
        if (isShowing)
        {
            wm.updateViewLayout(window,lp);
        }
    }
    public boolean isShowing()
    {
        return isShowing;
    }


    /*窗口设置类方法*/
    /*
    * 这几个事件最好是由窗口样式中的元素来设置，这样在更改内容View之后不需要再次设置
    * OnPressAction仅作用于设置的MoveView，拖动距离不远的话算作点击，
    * 该方法的调用在主线程，不要放置网络连接等操作，如果想取消点击事件，传入null即可
    * Move和Resize被限制在屏幕之内，这是为了保证窗口的稳定
    * 移动和改变大小事件推荐设置在Button上，TextView对touch事件的触发不完整
    * 只有touch_down事件触发了回调*/
    public void setMoveView(View v)
    {
        v.setOnTouchListener(listenerMove);
    }
    public void setMoveView(int ID)
    {
        View v = window.findViewById(ID);
        if(v != null)
        {
            v.setOnTouchListener(listenerMove);
        }
    }
    public void setResizeView(View v)
    {
        v.setOnTouchListener(listenerResize);
    }
    public void setResizeView(int ID)
    {
        View v = window.findViewById(ID);
        if(v != null)
        {
            v.setOnTouchListener(listenerResize);
        }
    }
    public void setOnPressAction(Runnable run)
    {
        onPressAction = run;
    }


    /*窗口行为类方法*/
    /*记得先resize再move
    * 为了保证所有按钮可用，这里的移动和调整大小的事件被限制为在屏幕之内*/
    public void moveTo(int x,int y)
    {
        lp.x = x<0?0:(x+lp.width<=scrWidth?x:scrWidth-lp.width);
        lp.y = y<0?0:(y+lp.height<=scrHeight?y:scrHeight-lp.height);
        if (isShowing)
        wm.updateViewLayout(window,lp);
    }
    public void moveBy(float xOffset,float yOffset)
    {
        lp.x =(int) (lp.x+xOffset<0?0:(lp.x+lp.width+xOffset<=scrWidth?lp.x+xOffset:scrWidth-lp.width));
        lp.y =(int) (lp.y+yOffset<0?0:(lp.y+lp.height+yOffset<=scrHeight?lp.y+yOffset:scrHeight-lp.height));
        if (isShowing)
        wm.updateViewLayout(window,lp);
    }
    public void resizeTo(int width,int height)
    {
        lp.width = width<minWidth?minWidth:(width<=maxWidth-16?width:maxWidth);
        lp.height = height<minHeight?minHeight:(height<=maxHeight-16?height:maxHeight);
        if(isShowing)
        wm.updateViewLayout(window,lp);
    }
    public void resizeBy(float xOffset,float yOffset)
    {
        lp.width =(int) (lp.width+xOffset<minWidth?minWidth:(lp.width+xOffset<=maxWidth?lp.width+xOffset:maxWidth));
        lp.height =(int) (lp.height+yOffset<minHeight?minHeight:(lp.height+yOffset<=maxHeight?lp.height+yOffset:maxHeight));
        if (isShowing)
        wm.updateViewLayout(window,lp);
    }
    public void show()
    {
        if (!isShowing)
        {
            isShowing = true;
            wm.addView(window, lp);
        }
    }
    public void hide()
    {
        if (isShowing)
        {
            isShowing = false;
            wm.removeView(window);
        }
    }

    /*设计或许会用到的dp转px*/
    public static int dp2px(Context context, float dipValue)
    {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale + 0.5f);
    }

    /*默认执行了移动和改变大小的点击事件后会移到最上层*/
    class TouchListenerMove implements View.OnTouchListener
    {
        float lX,lY;
        int startX,startY;
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    lX = motionEvent.getRawX();
                    lY = motionEvent.getRawY();
                    startX = lp.x;
                    startY = lp.y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    moveBy(motionEvent.getRawX()-lX,motionEvent.getRawY()-lY);
                    lX = motionEvent.getRawX();
                    lY = motionEvent.getRawY();
                    break;
                case MotionEvent.ACTION_UP:
                    lX = lY = 0;
                    if (Math.abs(lp.x-startX)<5&&Math.abs(lp.y-startY)<5)
                    {
                        moveTo(startX,startY);
                        if (onPressAction!=null)
                        {
                            onPressAction.run();
                        }
                    }
                    //hide();
                    //show();
                    break;
            }
            return false;
        }
    }
    class TouchListenerResize implements View.OnTouchListener
    {
        View tmpView;
        WindowManager.LayoutParams tmpLp = new WindowManager.LayoutParams();
        float lX,lY;
        private void resizeTmpView(float xOffset,float yOffset)
        {
            tmpLp.width =(int) (tmpLp.width+xOffset<minWidth?minWidth:(tmpLp.width+xOffset<=maxWidth?tmpLp.width+xOffset:maxWidth));
            tmpLp.height =(int) (tmpLp.height+yOffset<minHeight?minHeight:(tmpLp.height+yOffset<=maxHeight?tmpLp.height+yOffset:maxHeight));
            if (isShowing)
                wm.updateViewLayout(tmpView,tmpLp);
        }
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    lX = motionEvent.getRawX();
                    lY = motionEvent.getRawY();
                    if (tmpView==null)
                    {
                        tmpView = new View(context);
                        tmpView.setBackgroundColor(Color.parseColor("#800080FF"));
                    }
                    tmpLp.x = lp.x;
                    tmpLp.y = lp.y;
                    tmpLp.width = lp.width;
                    tmpLp.height = lp.height;
                    tmpLp.type = lp.type;
                    tmpLp.flags = lp.flags;
                    tmpLp.format = lp.format;
                    tmpLp.gravity = lp.gravity;
                    wm.addView(tmpView,tmpLp);
                    break;
                case MotionEvent.ACTION_MOVE:
                    resizeTmpView(motionEvent.getRawX()-lX,motionEvent.getRawY()-lY);
                    lX = motionEvent.getRawX();
                    lY = motionEvent.getRawY();
                    break;
                case MotionEvent.ACTION_UP:
                    lX = lY = 0;
                    resizeTo(tmpLp.width,tmpLp.height);
                    wm.removeView(tmpView);
                    //hide();
                    //show();
                    break;
            }
            return false;
        }
    }

}