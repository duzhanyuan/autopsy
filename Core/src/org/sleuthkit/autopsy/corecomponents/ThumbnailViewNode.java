/*
 * Autopsy Forensic Browser
 *
 * Copyright 2011 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.corecomponents;

import java.awt.Image;
import java.lang.ref.SoftReference;
import java.util.concurrent.ExecutionException;
import javax.swing.ImageIcon;
import javax.swing.SwingWorker;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.sleuthkit.autopsy.coreutils.ImageUtils;
import org.sleuthkit.datamodel.Content;

/**
 * Node that wraps around original node and adds the bitmap icon representing
 * the picture
 */
class ThumbnailViewNode extends FilterNode {

    private static final Image waitingIcon = new ImageIcon(ImageUtils.class.getResource("/org/sleuthkit/autopsy/images/Throbber_allbackgrounds_cyanblue.gif")).getImage();

    private SoftReference<Image> iconCache = null;
    private int iconSize = ImageUtils.ICON_SIZE_MEDIUM;
    //private final BufferedImage defaultIconBI;

   
    private SwingWorker<Image, Object> swingWorker;

    /**
     * the constructor
     */
    ThumbnailViewNode(Node arg, int iconSize) {
        super(arg, Children.LEAF);
        this.iconSize = iconSize;
    }

    @Override
    public String getDisplayName() {
        if (super.getDisplayName().length() > 15) {
            return super.getDisplayName().substring(0, 15).concat("...");
        } else {
            return super.getDisplayName();
        }
    }

    @Override
     public Image getIcon(int type) {
        Image icon = null;

        if (iconCache != null) {
            icon = iconCache.get();
        }

        if (icon != null) {
            return icon;
        } else {
            final Content content = this.getLookup().lookup(Content.class);
            if (content == null) {
                return ImageUtils.getDefaultIcon();
            }
            if (swingWorker == null || swingWorker.isDone()) {
                swingWorker = new SwingWorker<Image, Object>() {
                    final private ProgressHandle progressHandle = ProgressHandleFactory.createHandle("generating thumbnail for video file " + content.getName());

                    @Override
                    protected Image doInBackground() throws Exception {
                        progressHandle.start();
                        return ImageUtils.getIcon(content, iconSize);
                    }

                    @Override
                    protected void done() {
                        super.done();
                        try {
                            iconCache = new SoftReference<>(super.get());
                            progressHandle.finish();
                            fireIconChange();
                        } catch (InterruptedException | ExecutionException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                        swingWorker = null;
                    }
                };
                swingWorker.execute();
            }

            return waitingIcon;
        }
    }

    public void setIconSize(int iconSize) {
        this.iconSize = iconSize;
        iconCache = null;
    }

}
