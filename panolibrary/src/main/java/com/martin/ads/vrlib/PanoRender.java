package com.martin.ads.vrlib;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.martin.ads.vrlib.constant.AdjustingMode;
import com.martin.ads.vrlib.filters.base.AbsFilter;
import com.martin.ads.vrlib.filters.base.DrawImageFilter;
import com.martin.ads.vrlib.filters.base.FilterGroup;
import com.martin.ads.vrlib.filters.base.OESFilter;
import com.martin.ads.vrlib.filters.base.OrthoFilter;
import com.martin.ads.vrlib.filters.vr.Sphere2DPlugin;
import com.martin.ads.vrlib.utils.BitmapUtils;
import com.martin.ads.vrlib.utils.StatusHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Ads on 2016/6/25.
 */
public class PanoRender
        implements GLSurfaceView.Renderer {
    public static String TAG = "PanoRender";

    private StatusHelper statusHelper;
    private PanoMediaPlayerWrapper panoMediaPlayerWrapper;
    private Sphere2DPlugin spherePlugin;
    private FilterGroup filterGroup;
    private AbsFilter firstPassFilter;
    private int width,height;

    private boolean imageMode;
    private boolean planeMode;
    private boolean saveImg;

    private OrthoFilter orthoFilter;

    private PanoRender() {

    }

    public PanoRender init(){
        saveImg=false;
        filterGroup=new FilterGroup();

        if(!imageMode) {
            firstPassFilter = new OESFilter(statusHelper.getContext());
        }else{
            firstPassFilter=new DrawImageFilter(
                    statusHelper.getContext(),
                    //TODO: where is my UI?????
                    "filter/imgs/texture_360_n.jpg",
                    AdjustingMode.ADJUSTING_MODE_STRETCH);
        }
        filterGroup.addFilter(firstPassFilter);
        spherePlugin=new Sphere2DPlugin(statusHelper);
        if(!planeMode){
            filterGroup.addFilter(spherePlugin);
        }else{
            //TODO: this should be adjustable
            orthoFilter=new OrthoFilter(statusHelper.getContext(),
                    AdjustingMode.ADJUSTING_MODE_FIT_TO_SCREEN);
            if(panoMediaPlayerWrapper!=null){
                panoMediaPlayerWrapper.setVideoSizeCallback(new PanoMediaPlayerWrapper.VideoSizeCallback() {
                    @Override
                    public void notifyVideoSizeChanged(int width, int height) {
                        orthoFilter.updateProjection(width,height);
                    }
                });
                filterGroup.addFilter(orthoFilter);
            }
        }
        return this;
    }
    @Override
    public void onSurfaceCreated(GL10 glUnused,EGLConfig config) {
        filterGroup.init();
        if(!imageMode)
            panoMediaPlayerWrapper.setSurface(((OESFilter)firstPassFilter).getGlOESTexture().getTextureId());
    }


    @Override
    public void onDrawFrame(GL10 glUnused) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glFrontFace(GLES20.GL_CW);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        if(!imageMode){
            panoMediaPlayerWrapper.doTextureUpdate(((OESFilter)firstPassFilter).getSTMatrix());
            filterGroup.onDrawFrame(((OESFilter)firstPassFilter).getGlOESTexture().getTextureId());
        }else{
            //don't change the value, may cause crash
            filterGroup.onDrawFrame(0);
        }
        if (saveImg){
            BitmapUtils.sendImage(width,height,statusHelper.getContext());
            saveImg=false;
        }

        GLES20.glDisable(GLES20.GL_CULL_FACE);
        //GLES20.glFinish();
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        this.width=width;
        this.height=height;

        GLES20.glViewport(0,0,width,height);
        filterGroup.onFilterChanged(width,height);
    }

    public void saveImg(){
        saveImg=true;
    }

    public Sphere2DPlugin getSpherePlugin() {
        return spherePlugin;
    }

    public FilterGroup getFilterGroup() {
        return filterGroup;
    }

    public PanoRender setStatusHelper(StatusHelper statusHelper) {
        this.statusHelper = statusHelper;
        return this;
    }

    public PanoRender setPanoMediaPlayerWrapper(PanoMediaPlayerWrapper panoMediaPlayerWrapper) {
        this.panoMediaPlayerWrapper = panoMediaPlayerWrapper;
        return this;
    }

    public PanoRender setImageMode(boolean imageMode) {
        this.imageMode = imageMode;
        return this;
    }

    public PanoRender setPlaneMode(boolean planeMode) {
        this.planeMode = planeMode;
        return this;
    }

    public static PanoRender newInstance(){
        return new PanoRender();
    }
}