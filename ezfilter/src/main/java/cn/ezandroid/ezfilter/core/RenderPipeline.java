package cn.ezandroid.ezfilter.core;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cn.ezandroid.ezfilter.io.output.BufferOutput;
import cn.ezandroid.ezfilter.view.GLTextureView;

/**
 * 渲染管道
 * <p>
 * 一个完整的渲染管道由渲染起点，滤镜列表和渲染终点组成
 *
 * @author like
 * @date 2017-09-15
 */
public class RenderPipeline implements GLTextureView.Renderer {

    private boolean mIsRendering;

    private int mWidth;
    private int mHeight;

    private FBORender mStartPointRender; // 起点渲染器
    private List<FilterRender> mFilterRenders = new ArrayList<>(); // 滤镜列表
    private EndPointRender mEndPointRender = new EndPointRender(); // 终点渲染器

    private List<BufferOutput> mOutputs = new ArrayList<>(); // 输出列表

    private final List<AbstractRender> mRendersToDestroy;

    private int mCurrentRotation;

//    private List<OnSurfaceChangedListener> mOnSurfaceChangedListeners;
//    private List<OnSurfaceDestroyListener> mOnSurfaceDestroyListeners;

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
//        Log.e("RenderPipeline", this + " onSurfaceCreated:" + mWidth + "x" + mHeight);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
//        Log.e("RenderPipeline", this + " onSurfaceChanged:" + width + "x" + height);
        this.mWidth = width;
        this.mHeight = height;
        updateRenderSize();

//        for (OnSurfaceChangedListener listener : mOnSurfaceChangedListeners) {
//            listener.onSurfaceChanged(width, height);
//        }
//        mOnSurfaceChangedListeners.clear();
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
//        Log.e("RenderPipeline", this + " onDrawFrame:" + mWidth + "x" + mHeight + " " + isRendering());
        if (isRendering()) {
            if (mStartPointRender != null) {
                mStartPointRender.onDrawFrame();
            }
            synchronized (mRendersToDestroy) {
                for (AbstractRender renderer : mRendersToDestroy) {
                    if (renderer != null) {
                        renderer.destroy();
                    }
                }
                mRendersToDestroy.clear();
            }
        }
    }

    @Override
    public void onSurfaceDestroyed() {
//        Log.e("RenderPipeline", this + " onSurfaceDestroyed " + Thread.currentThread().getName());
        if (mStartPointRender != null) {
            mStartPointRender.destroy();
        }
        for (FilterRender filterRender : mFilterRenders) {
            filterRender.destroy();
        }
        mEndPointRender.destroy();

        for (BufferOutput bufferOutput : mOutputs) {
            bufferOutput.destroy();
        }
//        for (OnSurfaceDestroyListener listener : mOnSurfaceDestroyListeners) {
//            listener.onSurfaceDestroyed();
//        }
//        mOnSurfaceDestroyListeners.clear();
    }

//    public interface OnSurfaceChangedListener {
//
//        void onSurfaceChanged(int width, int height);
//    }
//
//    public interface OnSurfaceDestroyListener {
//
//        void onSurfaceDestroyed();
//    }

    public RenderPipeline() {
        mRendersToDestroy = new ArrayList<>();
//        mOnSurfaceChangedListeners = new ArrayList<>();
//        mOnSurfaceDestroyListeners = new ArrayList<>();
    }

    public void clean() {
        boolean isRenders = isRendering();
        setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

        if (mStartPointRender != null) {
            mStartPointRender.clearTargets();
        }
        addRenderToDestroy(mStartPointRender);
        mStartPointRender = null;

        for (FilterRender filterRender : mFilterRenders) {
            addRenderToDestroy(filterRender);
        }
        mFilterRenders.clear();

        for (BufferOutput bufferOutput : mOutputs) {
            addRenderToDestroy(bufferOutput);
        }
        mOutputs.clear();

        mCurrentRotation = 0;
        mEndPointRender.setRotate90Degrees(0);

        setRendering(isRenders);
    }

    private void updateRenderSize() {
        if (mStartPointRender != null) {
            mStartPointRender.setRenderSize(mWidth, mHeight);
        }
        for (FilterRender filterRender : mFilterRenders) {
            filterRender.setRenderSize(mWidth, mHeight);
        }
        mEndPointRender.setRenderSize(mWidth, mHeight);

        for (BufferOutput bufferOutput : mOutputs) {
            bufferOutput.setRenderSize(mWidth, mHeight);
        }
    }

    /**
     * 添加一个滤镜到销毁队列
     * 下一个onDrawFrame执行时，会调用销毁队列中的所有滤镜的destroy方法，并清空销毁队列
     *
     * @param render
     */
    public void addRenderToDestroy(AbstractRender render) {
        synchronized (mRendersToDestroy) {
            mRendersToDestroy.add(render);
        }
    }

//    /**
//     * 添加onSurfaceChanged监听
//     *
//     * @param listener
//     */
//    public void addOnSurfaceChangedListener(OnSurfaceChangedListener listener) {
//        if (mOnSurfaceChangedListeners.contains(listener)) {
//            mOnSurfaceChangedListeners.remove(listener);
//        }
//        mOnSurfaceChangedListeners.add(listener);
//    }
//
//    /**
//     * 添加onSurfaceDestroy监听
//     *
//     * @param listener
//     */
//    public void addOnSurfaceDestroyListeners(OnSurfaceDestroyListener listener) {
//        if (mOnSurfaceDestroyListeners.contains(listener)) {
//            mOnSurfaceDestroyListeners.remove(listener);
//        }
//        mOnSurfaceDestroyListeners.add(listener);
//    }

    public int getHeight() {
        return mHeight;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setRenderSize(int width, int height) {
        mWidth = width;
        mHeight = height;
        updateRenderSize();
    }

    /**
     * 设置顺时针旋转90度的次数
     * 取值-3~0，0~3，表示旋转-270~0，0~270度
     *
     * @param numOfTimes 旋转次数
     */
    public void setRotate90Degrees(int numOfTimes) {
        mCurrentRotation = numOfTimes;
        mEndPointRender.resetRotate();
        mEndPointRender.setRotate90Degrees(numOfTimes);

        for (BufferOutput bufferOutput : mOutputs) {
            bufferOutput.setRotate90Degrees(numOfTimes);
        }
    }

    /**
     * 是否正在渲染
     *
     * @return
     */
    public synchronized boolean isRendering() {
        return mIsRendering;
    }

    /**
     * 设置是否渲染
     *
     * @param rendering
     */
    public synchronized void setRendering(boolean rendering) {
        mIsRendering = rendering;
    }

    /**
     * 开始渲染
     */
    public synchronized void startRender() {
        mIsRendering = true;
    }

    /**
     * 暂停渲染
     */
    public synchronized void pauseRender() {
        mIsRendering = false;
    }

    /**
     * 获取渲染起点
     *
     * @return
     */
    public synchronized FBORender getStartPointRender() {
        return mStartPointRender;
    }

    /**
     * 设置渲染起点
     *
     * @param rootRenderer
     */
    public synchronized void setStartPointRender(FBORender rootRenderer) {
        if (mStartPointRender != null) {
            for (OnTextureAvailableListener render : mStartPointRender.getTargets()) {
                rootRenderer.addTarget(render);
            }
            mStartPointRender.clearTargets();
            addRenderToDestroy(mStartPointRender);
            mStartPointRender = rootRenderer;
            mStartPointRender.setRenderSize(mWidth, mHeight);
        } else {
            mStartPointRender = rootRenderer;
            mStartPointRender.setRenderSize(mWidth, mHeight);
            mStartPointRender.addTarget(mEndPointRender);
        }
        updateRenderSize();
    }

    public synchronized void addOutput(FBORender filterRender, BufferOutput bufferOutput) {
        if (bufferOutput != null && !mOutputs.contains(bufferOutput)
                && mStartPointRender != null && mEndPointRender != null) {
            boolean isRenders = isRendering();
            setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

            bufferOutput.clearTargets();
            bufferOutput.setRenderSize(mWidth, mHeight);
            bufferOutput.setRotate90Degrees(mCurrentRotation);

            filterRender.addTarget(bufferOutput);
            mOutputs.add(bufferOutput);

            setRendering(isRenders);
        }
    }

    public synchronized void removeOutput(FBORender filterRender, BufferOutput bufferOutput) {
        if (filterRender != null && mFilterRenders.contains(filterRender)
                && mStartPointRender != null && mEndPointRender != null) {
            boolean isRenders = isRendering();
            setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

            mOutputs.remove(bufferOutput);

            filterRender.removeTarget(bufferOutput);
            addRenderToDestroy(bufferOutput);

            setRendering(isRenders);
        }
    }

    /**
     * 添加滤镜
     *
     * @param filterRender
     */
    public synchronized void addFilterRender(FilterRender filterRender) {
        if (filterRender != null && !mFilterRenders.contains(filterRender)
                && mStartPointRender != null && mEndPointRender != null) {
            boolean isRenders = isRendering();
            setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

            filterRender.clearTargets(); // 确保要添加的滤镜是干净的
            filterRender.setRenderSize(mWidth, mHeight);

            if (mFilterRenders.isEmpty()) {
                // 添加了第一个滤镜
                mStartPointRender.removeTarget(mEndPointRender);
                mStartPointRender.addTarget(filterRender);
                filterRender.addTarget(mEndPointRender);
            } else {
                FilterRender lastFilterRender = mFilterRenders.get(mFilterRenders.size() - 1);
                lastFilterRender.removeTarget(mEndPointRender);
                lastFilterRender.addTarget(filterRender);
                filterRender.addTarget(mEndPointRender);
            }
            mFilterRenders.add(filterRender);

            setRendering(isRenders);
        }
    }

    /**
     * 删除滤镜
     *
     * @param filterRender
     */
    public synchronized void removeFilterRender(FilterRender filterRender) {
        if (filterRender != null && mFilterRenders.contains(filterRender)
                && mStartPointRender != null && mEndPointRender != null) {
            boolean isRenders = isRendering();
            setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

            int index = mFilterRenders.indexOf(filterRender);
            mFilterRenders.remove(filterRender);
            if (mFilterRenders.isEmpty()) {
                // 删除了最后一个滤镜
                mStartPointRender.removeTarget(filterRender);
                filterRender.removeTarget(mEndPointRender);
                mStartPointRender.addTarget(mEndPointRender);
            } else {
                if (index == 0) {
                    FilterRender nextRender = mFilterRenders.get(0);
                    mStartPointRender.removeTarget(filterRender);
                    filterRender.removeTarget(nextRender);
                    mStartPointRender.addTarget(nextRender);
                } else if (index == mFilterRenders.size()) {
                    FilterRender prevRender = mFilterRenders.get(mFilterRenders.size() - 1);
                    prevRender.removeTarget(filterRender);
                    filterRender.removeTarget(mEndPointRender);
                    prevRender.addTarget(mEndPointRender);
                } else {
                    FilterRender prevRender = mFilterRenders.get(index - 1);
                    FilterRender nextRender = mFilterRenders.get(index);
                    prevRender.removeTarget(filterRender);
                    filterRender.removeTarget(nextRender);
                    prevRender.addTarget(nextRender);
                }
            }
            addRenderToDestroy(filterRender);

            setRendering(isRenders);
        }
    }

    /**
     * 清空滤镜列表
     */
    public synchronized void clearFilterRenders() {
        if (mStartPointRender != null && mEndPointRender != null && !mFilterRenders.isEmpty()) {
            boolean isRenders = isRendering();
            setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

            if (mFilterRenders.size() == 1) {
                FilterRender filterRender = mFilterRenders.get(0);
                mStartPointRender.removeTarget(filterRender);
                filterRender.removeTarget(mEndPointRender);
                mStartPointRender.addTarget(mEndPointRender);
            } else {
                FilterRender firstFilterRender = mFilterRenders.get(0);
                FilterRender lastFilterRender = mFilterRenders.get(mFilterRenders.size() - 1);
                mStartPointRender.removeTarget(firstFilterRender);
                lastFilterRender.removeTarget(mEndPointRender);
                mStartPointRender.addTarget(mEndPointRender);
            }
            for (FilterRender filterRender : mFilterRenders) {
                addRenderToDestroy(filterRender);
            }
            mFilterRenders.clear();

            setRendering(isRenders);
        }
    }

    /**
     * 获取滤镜列表
     *
     * @return
     */
    public synchronized List<FilterRender> getFilterRenders() {
        return mFilterRenders;
    }

    /**
     * 获取渲染终点
     *
     * @return
     */
    public synchronized EndPointRender getEndPointRender() {
        return mEndPointRender;
    }
}