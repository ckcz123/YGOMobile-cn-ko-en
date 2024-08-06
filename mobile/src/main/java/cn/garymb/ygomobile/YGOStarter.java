package cn.garymb.ygomobile;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import cn.garymb.ygomobile.utils.DeckUtil;
import cn.garymb.ygomobile.utils.FileUtils;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.signature.MediaStoreSignature;

import java.io.File;
import java.net.URI;
import java.nio.channels.Channels;
import java.util.HashMap;

import cn.garymb.ygodata.YGOGameOptions;
import cn.garymb.ygomobile.core.IrrlichtBridge;
import cn.garymb.ygomobile.lite.R;
import cn.garymb.ygomobile.ui.plus.ViewTargetPlus;
import cn.garymb.ygomobile.utils.ComponentUtils;
import cn.garymb.ygomobile.utils.glide.GlideCompat;


public class YGOStarter {
    private static final String TAG = "YGOStarter";
    private static Bitmap mLogo;

    private static void setFullScreen(Activity activity, ActivityShowInfo activityShowInfo) {
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (activity instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
        } else {
            android.app.ActionBar actionBar = activity.getActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
        }
    }

    private static void quitFullScreen(Activity activity, ActivityShowInfo activityShowInfo) {
        if (activity instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
            if (activityShowInfo.hasSupperbar && actionBar != null) {
                actionBar.show();
            }
        } else {
            android.app.ActionBar actionBar = activity.getActionBar();
            if (activityShowInfo.hasBar && actionBar != null) {
                actionBar.show();
            }
        }
        final WindowManager.LayoutParams attrs = activity.getWindow().getAttributes();
        attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity.getWindow().setAttributes(attrs);
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private static void showLoadingBg(Activity activity) {
        ActivityShowInfo activityShowInfo = Infos.get(activity);
        if (activityShowInfo == null) {
            return;
        }
        activityShowInfo.isRunning = true;
        activityShowInfo.rootOld = activityShowInfo.mRoot.getBackground();
        activityShowInfo.mContentView.setVisibility(View.INVISIBLE);
        //读取当前的背景图，如果卡的话，可以考虑缓存bitmap
        File bgfile = new File(AppsSettings.get().getCoreSkinPath(), Constants.CORE_SKIN_BG);
        if (bgfile.exists()) {
//            .getApplicationContext()
            GlideCompat.with(activity).load(bgfile)
                    .signature(new MediaStoreSignature("image/*", bgfile.lastModified(), 0))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(activityShowInfo.mViewTarget);
        } else {
            GlideCompat.with(activity.getApplicationContext()).load(R.drawable.bg).into(activityShowInfo.mViewTarget);
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//强制为横屏
        setFullScreen(activity, activityShowInfo);
    }

    private static void hideLoadingBg(Activity activity, ActivityShowInfo activityShowInfo) {
        mLogo = null;
        activityShowInfo.mContentView.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= 16) {
            activityShowInfo.mRoot.setBackground(activityShowInfo.rootOld);
        } else {
            activityShowInfo.mRoot.setBackgroundDrawable(activityShowInfo.rootOld);
        }
        activity.setRequestedOrientation(activityShowInfo.oldRequestedOrientation);
        quitFullScreen(activity, activityShowInfo);
    }

    /**
     * 对添加用于展示的相关信息
     *
     * @param activity
     * @return
     */
    public static ActivityShowInfo onCreated(Activity activity) {
        ActivityShowInfo activityShowInfo = Infos.get(activity);
        if (activityShowInfo == null) {
            activityShowInfo = new ActivityShowInfo();
            Infos.put(activity, activityShowInfo);
//            Log.i("checker", "init:" + activity);
        }
        activityShowInfo.oldRequestedOrientation = activity.getRequestedOrientation();
//        Log.w("checker", "activityShowInfo.oldRequestedOrientation=" + activityShowInfo.oldRequestedOrientation);
        if (activityShowInfo.oldRequestedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            activityShowInfo.oldRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }
        activityShowInfo.mRoot = activity.getWindow().getDecorView();
        activityShowInfo.mViewTarget = new ViewTargetPlus(activityShowInfo.mRoot);
        activityShowInfo.mContentView = activityShowInfo.mRoot.findViewById(android.R.id.content);
        activityShowInfo.rootOld = activityShowInfo.mRoot.getBackground();
        if (activity instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
            if (actionBar != null) {
                activityShowInfo.hasSupperbar = actionBar.isShowing();
            }
        } else {
            android.app.ActionBar actionBar = activity.getActionBar();
            if (actionBar != null) {
                activityShowInfo.hasBar = actionBar.isShowing();
            }
        }
        return activityShowInfo;
    }

    public static void onDestroy(Activity activity) {
        Infos.remove(activity);
    }

    public static void onResumed(Activity activity) {
        ActivityShowInfo activityShowInfo = Infos.get(activity);
//        Log.i("checker", "resume:" + activity);
        if (activityShowInfo == null) {
            return;
        }
        if (!activityShowInfo.isFirst) {
            hideLoadingBg(activity, activityShowInfo);
        }
        activityShowInfo.isFirst = false;
        activityShowInfo.isRunning = false;
    }

    private static long lasttime = 0;

    /**
     * @param activity
     * @param options
     * @param args     例如(播放完退出游戏)：-r 1111.yrp
     *                 或者(播放完不退出游戏)：-k -r 1111.yrp
     */
    public static void startGame(Activity activity, YGOGameOptions options, String... args) {
        // Download card...
        DeckUtil.downloadInfo(activity);

        //如果距离上次加入游戏的时间大于1秒才处理
        if (System.currentTimeMillis() - lasttime >= 1000) {
            lasttime = System.currentTimeMillis();
            Log.e(TAG, "设置背景前" + System.currentTimeMillis());
            //显示加载背景
            showLoadingBg(activity);
            Log.e(TAG, "设置背景后" + System.currentTimeMillis());
        }
        Intent intent = new Intent(activity, YGOMobileActivity.class);
        if (options != null) {
            intent.putExtra(YGOGameOptions.YGO_GAME_OPTIONS_BUNDLE_KEY, options);
            intent.putExtra(YGOGameOptions.YGO_GAME_OPTIONS_BUNDLE_TIME, System.currentTimeMillis());
        }
        IrrlichtBridge.setArgs(intent, args);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Log.e(TAG, "跳转前" + System.currentTimeMillis());
        activity.startActivity(intent);
        Log.e(TAG, "跳转后" + System.currentTimeMillis());
    }

    /* 维护activity的用于展示的相关信息 */
    private static final HashMap<Activity, ActivityShowInfo> Infos = new HashMap<>();

    private static class ActivityShowInfo {
        //根布局
        View mRoot;
        ViewTargetPlus mViewTarget;
        //是否显示了标题栏
        boolean hasSupperbar;
        //是否显示了标题栏
        boolean hasBar;
        View mContentView;
        //activity背景
        Drawable rootOld;
        boolean isFirst = true;
        //屏幕方向
//        screenOrientations属性共有7中可选值(常量定义在 android.content.pm.ActivityInfo类中)：
//1.landscape：横屏(风景照)，显示时宽度大于高度；
//2.portrait：竖屏(肖像照)， 显示时高度大于宽度；
//3.user：用户当前的首选方向；
//4.behind：继承Activity堆栈中当前Activity下面的那个Activity的方向；
//5.sensor：由物理感应器决定显示方向，它取决于用户如何持有设备，当设备被旋转时方向会随之变化——在横屏与竖屏之间；
//6.nosensor：忽略物理感应器——即显示方向与物理感应器无关，不管用户如何旋转设备显示方向都不会随着改变("unspecified"设置除外)；
//7.unspecified：未指定，此为默认值，由Android系统自己选择适当的方向，选择策略视具体设备的配置情况而定，因此不同的设备会有不同的方向选择；
        int oldRequestedOrientation;
        boolean isRunning = false;
    }

    public static boolean isGameRunning(Context context) {
        return ComponentUtils.isProcessRunning(context, context.getPackageName() + ":game")
                && ComponentUtils.isActivityRunning(context, new ComponentName(context, YGOMobileActivity.class));
    }
}
