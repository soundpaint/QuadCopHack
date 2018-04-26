/*
 * @(#)ToolBar.java 1.00 17/01/27
 *
 * Copyright (C) 2017 Jürgen Reuter
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.soundpaint.qcapp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolBar;

public class ToolBar extends JToolBar
  implements TransportControl.Listener, DocumentManager.Listener
{
  private static final long serialVersionUID = -1621764458753767938L;

  final private QuadCopApp quadCopApp;
  final private TransportControl transportControl;
  final private DocumentManager documentManager;
  private JButton play;
  private JButton stop;
  private JButton record;

  private ToolBar()
  {
    throw new RuntimeException("unsupported constructor");
  }

  public ToolBar(final QuadCopApp quadCopApp)
  {
    if (quadCopApp == null) {
      throw new NullPointerException("qudCopApp");
    }
    this.quadCopApp = quadCopApp;
    transportControl = quadCopApp.getTransportControl();
    transportControl.addListener(this);
    documentManager = quadCopApp.getDocumentManager();
    documentManager.addListener(this);
    addButtons();
    fileAssociationChanged(null);
  }

  private void addButtons()
  {
    play = createToolButton("play32x32.png", "Start playing");
    play.setToolTipText("Start playing");
    play.addActionListener((final ActionEvent event) -> {
        transportControl.play(documentManager.getDocument());
      });
    play.setEnabled(false);
    add(play);

    stop = createToolButton("stop32x32.png", "Stop playing");
    stop.setToolTipText("Stop playing");
    stop.addActionListener((final ActionEvent event) -> {
        transportControl.stop();
      });
    stop.setEnabled(false);
    add(stop);

    record = createToolButton("record32x32.png", "Start recording");
    record.setToolTipText("Start recording");
    record.addActionListener((final ActionEvent event) -> {
        final Document document = documentManager.newDocument();
        if (document != null) {
          transportControl.record(document);
        } else {
          // record aborted
        }
      });
    record.setEnabled(true);
    add(record);
  }

  private static JButton createToolButton(final String imageFileName,
                                          final String altText)
  {
    final String imagePath = "/images/" + imageFileName;
    final URL imageURL = QuadCopApp.class.getResource(imagePath);
    final JButton button = new JButton();
    if (imageURL != null) {
      button.setIcon(new ImageIcon(imageURL, altText));
    } else {
      button.setText(altText);
      System.err.println("Resource not found: " + imagePath);
    }
    return button;
  }

  public void statusChanged(final TransportControl.Status oldStatus,
                            final TransportControl.Status newStatus)
  {
    switch (newStatus) {
    case STOPPED:
      stop.setEnabled(false);
      if (!documentManager.isDocumentEmpty() && transportControl.canPlay()) {
        play.setEnabled(true);
      }
      if (transportControl.canRecord()) {
        record.setEnabled(true);
      }
      break;
    case PLAYING:
      stop.setEnabled(true);
      play.setEnabled(false);
      record.setEnabled(false);
      break;
    case RECORDING:
      stop.setEnabled(true);
      play.setEnabled(false);
      record.setEnabled(false);
      break;
    default:
      throw new IllegalStateException("unknown transport control status");
    }
  }

  public void statusChanged(final DocumentManager.Status status,
                            final int documentSize)
  {
    switch (status) {
    case EMPTY_UNNAMED_DOCUMENT:
      stop.setEnabled(false);
      play.setEnabled(false);
      record.setEnabled(true);
      break;
    case MODIFIED_UNNAMED_DOCUMENT:
      stop.setEnabled(false);
      play.setEnabled(true);
      record.setEnabled(true);
      break;
    case MODIFIED_NAMED_DOCUMENT:
      stop.setEnabled(false);
      play.setEnabled(true);
      record.setEnabled(true);
      break;
    case UNMODIFIED_NAMED_DOCUMENT:
      stop.setEnabled(false);
      play.setEnabled(true);
      record.setEnabled(true);
      break;
    default:
      throw new IllegalStateException("unknown document status");
    }
  }

  public void fileAssociationChanged(final File file)
  {
    // ignored
  }

  public void playProgressChanged(final double progress,
                                  final String progressAsPercent,
                                  final int index,
                                  final int size)
  {
    // ignore
  }

  public void recordProgressChanged(final int size)
  {
    // ignore
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:java
 * End:
 */
