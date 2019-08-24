/*******************************************************************************
 * Copyright 2019 metaphore
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.crashinvaders.vfx.scene2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.utils.SnapshotArray;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.crashinvaders.vfx.VfxManager;
import com.crashinvaders.vfx.framebuffer.VfxFrameBuffer.BatchRendererAdapter;

public class VfxWidgetGroup extends WidgetGroup {

    private final VfxManager vfxManager;
    private final BatchRendererAdapter batchRendererAdapter;
    private boolean initialized = false;
    private boolean resizePending = false;

    public VfxWidgetGroup(Pixmap.Format pixelFormat) {
        vfxManager = new VfxManager(pixelFormat);
        batchRendererAdapter = new BatchRendererAdapter();
        super.setTransform(false);
    }

    public VfxManager getVfxManager() {
        return vfxManager;
    }

    @Override
    protected void setStage(Stage stage) {
        super.setStage(stage);

        if (stage != null) {
            initialize();
        } else {
            reset();
        }
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        resizePending = true;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.end();

        if (resizePending) {
            resizePending = false;
            vfxManager.resize(
                    MathUtils.floor(getWidth()),
                    MathUtils.floor(getHeight()));
        }

        vfxManager.cleanUpBuffers();

        vfxManager.getPingPongBuffer().addRenderer(batchRendererAdapter);
        vfxManager.beginCapture();

        batch.begin();

        validate();
        drawChildren(batch, parentAlpha);

        batch.end();

        vfxManager.endCapture();
        vfxManager.getPingPongBuffer().removeRenderer(batchRendererAdapter);

        vfxManager.applyEffects();

        batch.begin();

        // If something was captured, render result to the screen.
        if (vfxManager.hasResult()) {
            Color color = getColor();
            batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
            batch.draw(vfxManager.getResultBuffer().getFbo().getColorBufferTexture(),
                    getX(), getY(), getWidth(), getHeight(),
                    0f, 0f, 1f, 1f);
        }
    }

    @Override
    protected void drawChildren(Batch batch, float parentAlpha) {
        boolean capturing = vfxManager.isCapturing();

        if (capturing) {
            // Imitate "transform" child drawing for when capturing into VfxManager.
            super.setTransform(true);
        }
        if (!capturing) {
            // Clip children to VfxWidget area when not capturing into FBO.
            clipBegin();
        }

        super.drawChildren(batch, parentAlpha);
        batch.flush();

        if (capturing) {
            super.setTransform(false);
        }

        if (!capturing) {
            clipEnd();
        }
    }

    @Deprecated
    @Override
    public void setCullingArea(Rectangle cullingArea) {
        throw new UnsupportedOperationException("VfxWidgetGroup doesn't support culling area.");
    }

    @Deprecated
    @Override
    public void setTransform(boolean transform) {
        throw new UnsupportedOperationException("VfxWidgetGroup doesn't support transform.");
    }

    private void initialize() {
        if (initialized) return;

        int width = (int)getWidth();
        int height = (int)getHeight();
        if (width == 0 || height == 0) {
            Viewport viewport = getStage().getViewport();
            width = MathUtils.floor(viewport.getWorldWidth());
            height = MathUtils.floor(viewport.getWorldHeight());
        }
        vfxManager.resize(width, height);

        batchRendererAdapter.initialize(getStage().getBatch());

        resizePending = false;
        initialized = true;
    }

    private void reset() {
        if (!initialized) return;

        vfxManager.dispose();

        batchRendererAdapter.reset();

        resizePending = false;
        initialized = false;
    }
}
