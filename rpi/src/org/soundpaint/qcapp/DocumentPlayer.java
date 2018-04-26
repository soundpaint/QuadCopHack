/*
 * @(#)DocumentPlayer.java 1.00 17/01/26
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

import java.util.ArrayList;
import java.util.List;

public class DocumentPlayer implements QCPlayer
{
  private final Document document;
  private final List<ProgressListener> progressListeners;
  private int index;

  private DocumentPlayer()
  {
    throw new RuntimeException("unsupported constructor");
  }

  public DocumentPlayer(final Document document)
  {
    if (document == null) {
      throw new NullPointerException("document");
    }
    this.document = document;
    progressListeners = new ArrayList<ProgressListener>();
    index = 0;
  }

  public boolean addProgressListener(final ProgressListener progressListener)
  {
    final boolean success = progressListeners.add(progressListener);
    progressListener.progressChanged(getProgress(), getProgressAsString(),
                                     index, document.size());
    return success;
  }

  public boolean removeProgressListener(final ProgressListener progressListener)
  {
    return progressListeners.remove(progressListener);
  }

  private void endOfStreamReached()
  {
    for (final ProgressListener progressListener : progressListeners) {
      progressListener.endOfStreamReached();
    }
  }

  private void progressChanged()
  {
    for (final ProgressListener progressListener : progressListeners) {
      progressListener.progressChanged(getProgress(), getProgressAsString(),
                                       index, document.size());
    }
  }

  public double getProgress()
  {
    return (1.0 * index) / document.size();
  }

  public String getProgressAsString()
  {
    final int size = document.size();
    final int progress = size > 0 ? (10000 * index) / size : 10000;
    final int intPart = progress / 100;
    final int fractionPart = progress % 100;
    return String.format("%d.%02d%%", intPart, fractionPart);
  }

  public int available()
  {
    return document.size() - index;
  }

  public int provideNextChunk(final QuadCop.DataRecord[] buffer)
  {
    final int preferredNextIndex = index + buffer.length;
    final int maxNextIndex = document.size();
    final int nextIndex =
      preferredNextIndex <= maxNextIndex ? preferredNextIndex : maxNextIndex;
    final int copied = document.copyTo(buffer, index, nextIndex);
    index = nextIndex;
    progressChanged();
    if (available() == 0) {
      endOfStreamReached();
    }
    return copied;
  }

  private static final long RE_REPORT_BUFFER_UNDERRUN_ONLY_AFTER_MS = 1000;
  private long lastBufferUnderrunReported = 0;

  public void bufferUnderrunDetected()
  {
    final long bufferUnderrunReported = System.currentTimeMillis();
    if (bufferUnderrunReported - lastBufferUnderrunReported >
        RE_REPORT_BUFFER_UNDERRUN_ONLY_AFTER_MS) {
      System.err.println("*** document player: buffer underrun ***");
      lastBufferUnderrunReported = bufferUnderrunReported;
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:java
 * End:
 */
