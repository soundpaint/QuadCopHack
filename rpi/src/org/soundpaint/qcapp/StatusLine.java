/*
 * @(#)StatusLine.java 1.00 17/01/27
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

import java.awt.BorderLayout;
import java.io.File;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class StatusLine extends JPanel
  implements TransportControl.Listener, DocumentManager.Listener
{
  private static final long serialVersionUID = 4552304732979762187L;

  final private QuadCopApp quadCopApp;
  final private TransportControl transportControl;
  final private DocumentManager documentManager;
  final private JLabel message;
  final private JLabel documentSize;
  final private JLabel playProgress;

  private StatusLine()
  {
    throw new RuntimeException("unsupported constructor");
  }

  public StatusLine(final QuadCopApp quadCopApp)
  {
    if (quadCopApp == null) {
      throw new NullPointerException("qudCopApp");
    }
    this.quadCopApp = quadCopApp;
    transportControl = quadCopApp.getTransportControl();
    transportControl.addListener(this);
    documentManager = quadCopApp.getDocumentManager();
    documentManager.addListener(this);
    message = new JLabel();
    message.setText("Welcome to the Quad Cop App!");
    add(message, BorderLayout.WEST);
    playProgress = new JLabel();
    playProgress.setText("");
    add(playProgress);
    documentSize = new JLabel();
    documentSize.setText("");
    add(documentSize, BorderLayout.EAST);
  }

  public void playProgressChanged(final double progress,
                                  final String progressAsPercent,
                                  final int index,
                                  final int size)
  {
    playProgress.setText(progressAsPercent + " (" + index + "/" + size + ")");
  }

  public void recordProgressChanged(final int size)
  {
    documentSize.setText(size + " records");
  }

  public void statusChanged(final TransportControl.Status oldStatus,
                            final TransportControl.Status newStatus)
  {
    switch (newStatus) {
    case STOPPED:
      if (oldStatus == TransportControl.Status.RECORDING) {
        message.setText("Stopped recording.");
      } else if (oldStatus == TransportControl.Status.PLAYING) {
        message.setText("Stopped playing.");
      } else {
        message.setText("Stopped.");
      }
      break;
    case PLAYING:
      message.setText("Started playing.");
      break;
    case RECORDING:
      message.setText("Started recording.");
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
      message.setText("No document loaded.");
      break;
    case MODIFIED_UNNAMED_DOCUMENT:
      message.setText("There are unsaved changes.");
      break;
    case MODIFIED_NAMED_DOCUMENT:
      // message.setText("There are unsaved changes to the document.");
      break;
    case UNMODIFIED_NAMED_DOCUMENT:
      message.setText("The document has no unsaved changes.");
      break;
    default:
      throw new IllegalStateException("unknown document status");
    }
    if (status == DocumentManager.Status.EMPTY_UNNAMED_DOCUMENT) {
      this.documentSize.setText("");
      playProgress.setText("");
    } else {
      this.documentSize.setText(documentSize + " records");
    }
  }

  public void fileAssociationChanged(final File file)
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
